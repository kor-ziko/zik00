package com.zik00.shop.service.product.extractor;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.dto.product.ProductOptionResponse;
import com.zik00.shop.dto.product.ProductVariantResponse;
import com.zik00.shop.service.product.ProductImageSourceRegistry;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.io.ByteArrayInputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ProductPageExtractor {
    private static final Logger log = LoggerFactory.getLogger(ProductPageExtractor.class);
    private static final int MAX_HTML_BYTES = 2_000_000;

    private final OllamaProductMetadataExtractor aiExtractor;
    private final ProductImageSourceRegistry imageSourceRegistry;
    private final RenderedProductPageFetcher renderedPageFetcher;
    private final long defaultDomesticShippingFee;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.NORMAL)
            .connectTimeout(Duration.ofSeconds(8))
            .build();

    public ProductPageExtractor(
            OllamaProductMetadataExtractor aiExtractor,
            ProductImageSourceRegistry imageSourceRegistry,
            RenderedProductPageFetcher renderedPageFetcher
    ) {
        this(aiExtractor, imageSourceRegistry, renderedPageFetcher, 3_000L);
    }

    @Autowired
    public ProductPageExtractor(
            OllamaProductMetadataExtractor aiExtractor,
            ProductImageSourceRegistry imageSourceRegistry,
            RenderedProductPageFetcher renderedPageFetcher,
            @Value("${shop.pricing.default-domestic-shipping-fee-krw:3000}") long defaultDomesticShippingFee
    ) {
        this.aiExtractor = aiExtractor;
        this.imageSourceRegistry = imageSourceRegistry;
        this.renderedPageFetcher = renderedPageFetcher;
        this.defaultDomesticShippingFee = Math.max(0, defaultDomesticShippingFee);
    }

    public ProductDetailResponse extract(DiscoveredProduct product) {
        Optional<Document> renderedPage = renderedPageFetcher.fetch(product.sourceUrl());
        Optional<Document> page = renderedPage.or(() -> fetch(product.sourceUrl()));
        ExtractedProductMetadata structured = page.map(this::structuredMetadata)
                .orElseGet(ExtractedProductMetadata::empty);
        ExtractedProductMetadata embedded = page.map(this::embeddedMetadata)
                .orElseGet(ExtractedProductMetadata::empty);
        ExtractedProductMetadata openGraph = page.map(this::openGraphMetadata)
                .orElseGet(ExtractedProductMetadata::empty);
        Long pageShippingFee = page.map(this::domesticShippingFee).orElse(null);
        RenderedOptionCatalog renderedOptions = page.map(this::renderedOptions)
                .orElseGet(RenderedOptionCatalog::empty);
        if (renderedOptions.options().isEmpty()) {
            long basePrice = firstPositive(structured.price(), embedded.price(), renderedOptions.price(), product.price());
            renderedOptions = page.map(document -> embeddedOptions(document, basePrice))
                    .orElseGet(RenderedOptionCatalog::empty);
            if (renderedOptions.options().isEmpty() && renderedPage.isPresent()) {
                renderedOptions = fetch(product.sourceUrl())
                        .map(document -> embeddedOptions(document, basePrice))
                        .orElseGet(RenderedOptionCatalog::empty);
            }
        }
        boolean needsAiOptionFallback = renderedOptions.options().isEmpty()
                || (structured.price() == null && embedded.price() == null
                && renderedOptions.price() == null && product.price() <= 0)
                || firstNonNegative(structured.domesticShippingFee(), embedded.domesticShippingFee(), pageShippingFee) == null;
        ExtractedProductMetadata ai = page
                .filter(ignored -> needsAiOptionFallback)
                .flatMap(document -> aiExtractor.extract(aiInput(document), product.name()))
                .orElseGet(ExtractedProductMetadata::empty);

        String name = first(structured.name(), embedded.name(), openGraph.name(), ai.name(), product.name());
        String brand = first(structured.brand(), embedded.brand(), ai.brand(), product.brand(), "브랜드 정보 없음");
        String description = first(structured.description(), embedded.description(), openGraph.description(), ai.description(), product.description());
        long price = firstPositive(structured.price(), embedded.price(), renderedOptions.price(), product.price(), ai.price());
        Long originalPrice = firstPositiveNullable(structured.originalPrice(), embedded.originalPrice(),
                renderedOptions.originalPrice(), product.originalPrice(), ai.originalPrice());
        String currency = first(structured.currency(), embedded.currency(), product.currency(), ai.currency(), "KRW");
        Long extractedShippingFee = firstNonNegative(
                structured.domesticShippingFee(), embedded.domesticShippingFee(), pageShippingFee,
                ai.domesticShippingFee()
        );
        boolean shippingFeeEstimated = extractedShippingFee == null;
        long shippingFee = shippingFeeEstimated ? defaultDomesticShippingFee : extractedShippingFee;
        List<String> images = new ArrayList<>(mergedImages(structured, embedded, openGraph, ai));
        if (images.isEmpty() && product.imageUrl() != null && !product.imageUrl().isBlank()) images.add(product.imageUrl());
        images = normalizeImageUrls(images, product.sourceUrl());
        String thumbnail = images.isEmpty() ? product.imageUrl() : images.getFirst();
        List<ProductVariantResponse> variants = toVariants(
                renderedOptions.variants().isEmpty() ? ai.variants() : renderedOptions.variants()
        );
        List<ProductOptionResponse> options = toOptions(
                renderedOptions.options().isEmpty() ? ai.options() : renderedOptions.options(), variants
        );
        if (options.isEmpty() && product.options() != null) {
            options = product.options().entrySet().stream()
                    .filter(entry -> entry.getKey() != null && !entry.getKey().isBlank()
                            && entry.getValue() != null && !entry.getValue().isEmpty())
                    .map(entry -> new ProductOptionResponse(entry.getKey(), entry.getValue()))
                    .toList();
        }

        return new ProductDetailResponse(
                product.productId(), product.sourceUrl(), name, product.category(), price, originalPrice,
                proxyImageUrl(thumbnail), images.stream().map(this::proxyImageUrl).toList(),
                brand, description, currency, shippingFee, shippingFeeEstimated,
                product.rating(), product.reviewCount(),
                options, variants, List.of()
        );
    }

    private RenderedOptionCatalog renderedOptions(Document document) {
        Element marker = document.getElementById("zik00-rendered-options");
        if (marker == null || marker.data().isBlank()) return RenderedOptionCatalog.empty();
        try {
            return objectMapper.readValue(marker.data(), RenderedOptionCatalog.class);
        } catch (Exception exception) {
            log.info("렌더링된 상품 옵션을 해석하지 못했습니다: {}", exception.getMessage());
            return RenderedOptionCatalog.empty();
        }
    }

    private RenderedOptionCatalog embeddedOptions(Document document, long basePrice) {
        for (Element script : document.select("script[type=application/json], script#__NEXT_DATA__")) {
            try {
                JsonNode option = findEmbeddedOptionNode(objectMapper.readTree(script.data()));
                if (option == null) continue;
                List<String> labels = new ArrayList<>();
                for (JsonNode label : option.path("label")) labels.add(label.asString());
                if (labels.isEmpty()) continue;
                if (labels.size() == 1 && "옵션".equals(labels.getFirst())
                        && option.path("items").values().stream()
                        .map(item -> item.path("attributes").path(0).asString())
                        .filter(value -> !value.isBlank())
                        .allMatch(value -> value.matches("(?:1[8-9]\\d|[23]\\d{2})"))) {
                    labels.set(0, "사이즈");
                }

                List<ExtractedProductVariant> variants = new ArrayList<>();
                for (JsonNode item : option.path("items")) {
                    JsonNode values = item.path("attributes");
                    if (!values.isArray() || values.isEmpty()) continue;
                    Map<String, String> attributes = new LinkedHashMap<>();
                    for (int index = 0; index < Math.min(labels.size(), values.size()); index++) {
                        attributes.put(labels.get(index), values.get(index).asString());
                    }
                    long additionalPrice = item.path("price").asLong(0);
                    Long variantPrice = additionalPrice == 0 ? null : basePrice + additionalPrice;
                    boolean available = item.path("stock").asLong(1) > 0
                            && !"STOP".equalsIgnoreCase(item.path("status").asString());
                    variants.add(new ExtractedProductVariant(
                            item.path("id").asString(), attributes, variantPrice, available
                    ));
                }
                if (variants.isEmpty()) continue;
                Map<String, LinkedHashSet<String>> valuesByType = new LinkedHashMap<>();
                variants.forEach(variant -> variant.attributes().forEach((type, value) ->
                        valuesByType.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(value)));
                List<ExtractedProductOption> options = valuesByType.entrySet().stream()
                        .map(entry -> new ExtractedProductOption(entry.getKey(), List.copyOf(entry.getValue())))
                        .toList();
                return new RenderedOptionCatalog(options, variants, null, null);
            } catch (Exception exception) {
                log.info("페이지 내장 상품 옵션을 해석하지 못했습니다: {}", exception.getMessage());
            }
        }
        return RenderedOptionCatalog.empty();
    }

    private JsonNode findEmbeddedOptionNode(JsonNode node) {
        if (node == null || node.isMissingNode()) return null;
        if (node.isObject() && node.path("label").isArray() && !node.path("label").isEmpty()
                && node.path("items").isArray() && node.path("items").values().stream()
                .anyMatch(item -> item.path("attributes").isArray())) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = findEmbeddedOptionNode(child);
            if (found != null) return found;
        }
        return null;
    }

    private Optional<Document> fetch(String sourceUrl) {
        try {
            URI uri = validatePublicHttpUrl(sourceUrl);
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(20))
                    .header("Accept", "text/html,application/xhtml+xml")
                    .header("Accept-Language", "ko-KR,ko;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
                            + "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/144.0.0.0 Safari/537.36")
                    .GET()
                    .build();
            HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return Optional.empty();
            try (InputStream body = response.body()) {
                byte[] html = body.readNBytes(MAX_HTML_BYTES);
                return Optional.of(Jsoup.parse(new ByteArrayInputStream(html), null, uri.toString()));
            }
        } catch (Exception exception) {
            log.info("원본 상품 페이지를 읽지 못해 검색 결과 정보를 사용합니다: {}", exception.getMessage());
            return Optional.empty();
        }
    }

    private ExtractedProductMetadata structuredMetadata(Document document) {
        for (Element script : document.select("script[type=application/ld+json]")) {
            try {
                JsonNode product = findProductNode(objectMapper.readTree(script.data()));
                if (product == null) continue;
                JsonNode offer = firstOffer(product.path("offers"));
                List<String> images = imageUrls(product.path("image"));
                return new ExtractedProductMetadata(
                        product.path("name").asString(), brandName(product.path("brand")),
                        product.path("description").asString(), number(offer.path("price")),
                        number(offer.path("highPrice")), offer.path("priceCurrency").asString(),
                        structuredShippingFee(offer),
                        images.isEmpty() ? "" : images.getFirst(), images, List.of(), List.of()
                );
            } catch (Exception ignored) {
                // Other JSON-LD blocks on the same page may still contain the product.
            }
        }
        return ExtractedProductMetadata.empty();
    }

    private ExtractedProductMetadata embeddedMetadata(Document document) {
        for (Element script : document.select("script[type=application/json], script#__NEXT_DATA__")) {
            try {
                JsonNode product = findEmbeddedProductNode(objectMapper.readTree(script.data()));
                if (product == null) continue;
                List<String> images = new ArrayList<>();
                for (JsonNode thumbnail : product.path("thumbnails")) {
                    String url = thumbnail.isTextual() ? thumbnail.asString() : thumbnail.path("url").asString();
                    if (!url.isBlank()) images.add(url);
                }
                if (images.isEmpty()) {
                    images.addAll(imageUrls(product.path("images")));
                    images.addAll(imageUrls(product.path("image")));
                }
                Long sellingPrice = firstPositiveNullable(
                        number(product.path("sellPrice")), number(product.path("salePrice")),
                        number(product.path("discountedPrice")), number(product.path("price"))
                );
                Long originalPrice = firstPositiveNullable(
                        number(product.path("originalSellPrice")), number(product.path("originalPrice"))
                );
                return new ExtractedProductMetadata(
                        product.path("name").asString(),
                        first(product.path("brandName").asString(), brandName(product.path("brand"))),
                        product.path("description").asString(), sellingPrice, originalPrice,
                        first(product.path("currency").asString(), "KRW"), embeddedShippingFee(product),
                        images.isEmpty() ? "" : images.getFirst(), images, List.of(), List.of()
                );
            } catch (Exception exception) {
                log.info("페이지 내장 상품 정보를 해석하지 못했습니다: {}", exception.getMessage());
            }
        }
        return ExtractedProductMetadata.empty();
    }

    private JsonNode findEmbeddedProductNode(JsonNode node) {
        if (node == null || node.isMissingNode()) return null;
        boolean hasName = !node.path("name").asString().isBlank();
        boolean hasPrice = number(node.path("sellPrice")) != null || number(node.path("salePrice")) != null
                || number(node.path("discountedPrice")) != null || number(node.path("price")) != null;
        boolean hasImages = node.path("thumbnails").isArray() || node.path("images").isArray()
                || !node.path("image").isMissingNode();
        if (node.isObject() && hasName && hasPrice && hasImages) return node;
        for (JsonNode child : node) {
            JsonNode found = findEmbeddedProductNode(child);
            if (found != null) return found;
        }
        return null;
    }

    private ExtractedProductMetadata openGraphMetadata(Document document) {
        String image = meta(document, "og:image");
        return new ExtractedProductMetadata(
                first(meta(document, "og:title"), document.title()), "", meta(document, "og:description"),
                number(meta(document, "product:price:amount")), null,
                meta(document, "product:price:currency"), null, image,
                image.isBlank() ? List.of() : List.of(image), List.of(), List.of()
        );
    }

    private Long structuredShippingFee(JsonNode offer) {
        JsonNode details = offer.path("shippingDetails");
        if (details.isArray() && !details.isEmpty()) details = details.get(0);
        JsonNode rate = details.path("shippingRate");
        return firstNonNegative(number(rate.path("value")), number(rate.path("price")), number(rate));
    }

    private Long embeddedShippingFee(JsonNode product) {
        return firstNonNegative(
                number(product.path("shippingFee")), number(product.path("deliveryFee")),
                number(product.path("shippingCost")), number(product.path("deliveryCost")),
                number(product.path("baseShippingFee")), number(product.path("defaultDeliveryFee"))
        );
    }

    private Long domesticShippingFee(Document document) {
        String shippingText = document.select(
                        "[class*=shipping], [id*=shipping], [class*=delivery], [id*=delivery], "
                                + "[class*=deli], [id*=deli], [class*=ship], [id*=ship]"
                ).stream().limit(80).map(Element::text).reduce("", (left, right) -> left + " " + right);
        Long parsed = shippingFeeFromText(shippingText);
        return parsed != null ? parsed : shippingFeeFromText(document.body().text());
    }

    private Long shippingFeeFromText(String text) {
        if (text == null || text.isBlank()) return null;
        Matcher matcher = Pattern.compile(
                "(?:배송비|배송료|국내배송)[^0-9무료]{0,20}(무료|0\\s*원|[0-9][0-9,]{2,}\\s*원)",
                Pattern.CASE_INSENSITIVE
        ).matcher(text);
        if (!matcher.find()) return null;
        String value = matcher.group(1);
        if (value.contains("무료") || value.replaceAll("[^0-9]", "").equals("0")) return 0L;
        return number(value);
    }

    private String aiInput(Document document) {
        StringBuilder input = new StringBuilder();
        input.append("화면에 표시된 상품 옵션:\n");
        document.select("select, button, [role=option], [role=radio], [class*=option], [id*=option], [class*=size], [id*=size]")
                .stream()
                .limit(80)
                .map(Element::text)
                .map(String::strip)
                .filter(value -> !value.isBlank() && value.length() <= 300)
                .forEach(value -> input.append(value).append('\n'));
        input.append("\n옵션 및 가격 관련 페이지 데이터:\n");
        int scriptCharacters = 0;
        for (Element script : document.select("script")) {
            String data = script.data();
            String lower = data.toLowerCase(java.util.Locale.ROOT);
            if (data.isBlank() || !(lower.contains("variant") || lower.contains("sku")
                    || lower.contains("option") || lower.contains("size") || lower.contains("price"))) continue;
            int remaining = 8_000 - scriptCharacters;
            if (remaining <= 0) break;
            String excerpt = data.substring(0, Math.min(data.length(), Math.min(remaining, 8_000)));
            input.append(excerpt).append('\n');
            scriptCharacters += excerpt.length();
        }
        input.append("\n전체 페이지 텍스트:\n").append(document.body().text());
        return input.toString();
    }

    private List<ProductOptionResponse> toOptions(
            List<ExtractedProductOption> extracted,
            List<ProductVariantResponse> variants
    ) {
        Map<String, LinkedHashSet<String>> valuesByType = new LinkedHashMap<>();
        if (extracted != null) {
            for (ExtractedProductOption option : extracted) {
                if (option == null || option.optionType() == null || option.optionType().isBlank()) continue;
                String type = repairMojibake(option.optionType().strip());
                if (!isProductOptionType(type)) continue;
                LinkedHashSet<String> values = valuesByType.computeIfAbsent(type, ignored -> new LinkedHashSet<>());
                if (option.values() != null) option.values().stream()
                        .filter(this::isProductOptionValue)
                        .map(value -> repairMojibake(value.strip()))
                        .limit(30)
                        .forEach(values::add);
            }
        }
        for (ProductVariantResponse variant : variants) {
            variant.attributes().forEach((type, value) -> {
                if (isProductOptionType(type) && isProductOptionValue(value)) {
                    valuesByType.computeIfAbsent(type, ignored -> new LinkedHashSet<>()).add(value);
                }
            });
        }
        return valuesByType.entrySet().stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .limit(5)
                .map(entry -> new ProductOptionResponse(entry.getKey(), entry.getValue().stream().limit(30).toList()))
                .toList();
    }

    private List<ProductVariantResponse> toVariants(List<ExtractedProductVariant> extracted) {
        if (extracted == null) return List.of();
        java.util.concurrent.atomic.AtomicInteger sequence = new java.util.concurrent.atomic.AtomicInteger();
        return extracted.stream()
                .filter(variant -> variant != null && variant.attributes() != null && !variant.attributes().isEmpty())
                .limit(200)
                .map(variant -> {
                    Map<String, String> attributes = new LinkedHashMap<>();
                    variant.attributes().forEach((type, value) -> {
                        String normalizedType = type == null ? "" : repairMojibake(type.strip());
                        if (isProductOptionType(normalizedType) && isProductOptionValue(value)) {
                            attributes.put(normalizedType, repairMojibake(value.strip()));
                        }
                    });
                    String variantId = first(variant.variantId(), "VARIANT-" + sequence.incrementAndGet());
                    Long variantPrice = variant.price() != null && variant.price() > 0 ? variant.price() : null;
                    return new ProductVariantResponse(
                            variantId, Map.copyOf(attributes), variantPrice,
                            variant.available() == null || variant.available()
                    );
                })
                .filter(variant -> !variant.attributes().isEmpty())
                .toList();
    }

    private JsonNode findProductNode(JsonNode node) {
        if (node == null || node.isMissingNode()) return null;
        JsonNode type = node.path("@type");
        if ((type.isTextual() && "product".equalsIgnoreCase(type.asString()))
                || (type.isArray() && type.values().stream()
                .anyMatch(value -> "product".equalsIgnoreCase(value.asString())))) {
            return node;
        }
        for (JsonNode child : node) {
            JsonNode found = findProductNode(child);
            if (found != null) return found;
        }
        return null;
    }

    private JsonNode firstOffer(JsonNode offers) {
        return offers.isArray() && !offers.isEmpty() ? offers.get(0) : offers;
    }

    private List<String> imageUrls(JsonNode image) {
        if (image.isTextual()) return image.asString().isBlank() ? List.of() : List.of(image.asString());
        if (image.isObject()) {
            String url = first(image.path("url").asString(), image.path("contentUrl").asString());
            return url.isBlank() ? List.of() : List.of(url);
        }
        if (!image.isArray()) return List.of();
        List<String> values = new ArrayList<>();
        for (JsonNode item : image) values.addAll(imageUrls(item));
        return values;
    }

    private String brandName(JsonNode brand) {
        return brand.isTextual() ? brand.asString() : brand.path("name").asString();
    }

    private String meta(Document document, String property) {
        Element element = document.selectFirst("meta[property='" + property + "'], meta[name='" + property + "']");
        return element == null ? "" : element.attr("content").strip();
    }

    private Long number(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return null;
        if (node.isNumber()) return Math.round(node.asDouble());
        return number(node.asString());
    }

    private Long number(String value) {
        if (value == null || value.isBlank()) return null;
        String normalized = value.replaceAll("[^0-9.]", "");
        if (normalized.isBlank()) return null;
        try {
            return Math.round(Double.parseDouble(normalized));
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private List<String> mergedImages(ExtractedProductMetadata... sources) {
        Set<String> images = new LinkedHashSet<>();
        for (ExtractedProductMetadata source : sources) {
            if (source.image() != null && !source.image().isBlank()) images.add(source.image());
            if (source.images() != null) source.images().stream().filter(value -> value != null && !value.isBlank()).forEach(images::add);
        }
        return List.copyOf(images);
    }

    private List<String> normalizeImageUrls(List<String> candidates, String sourceUrl) {
        Map<String, String> imagesByIdentity = new LinkedHashMap<>();
        URI baseUri;
        try {
            baseUri = URI.create(sourceUrl);
        } catch (Exception ignored) {
            baseUri = null;
        }
        for (String candidate : candidates) {
            if (candidate == null || candidate.isBlank()) continue;
            try {
                String value = candidate.strip();
                if (value.startsWith("//")) value = "https:" + value;
                else if (value.matches("^[A-Za-z0-9.-]+\\.[A-Za-z]{2,}/.*")) value = "https://" + value;
                URI imageUri = URI.create(value);
                if (!imageUri.isAbsolute() && baseUri != null) imageUri = baseUri.resolve(imageUri);
                if (!("http".equalsIgnoreCase(imageUri.getScheme()) || "https".equalsIgnoreCase(imageUri.getScheme()))
                        || imageUri.getHost() == null) continue;
                String normalized = imageUri.toString();
                imagesByIdentity.putIfAbsent(imageIdentity(imageUri), normalized);
            } catch (Exception ignored) {
                // Invalid image candidates are ignored while other product images remain usable.
            }
        }
        return List.copyOf(imagesByIdentity.values());
    }

    private String imageIdentity(URI imageUri) {
        String host = imageUri.getHost().toLowerCase(java.util.Locale.ROOT);
        String path = imageUri.getPath();
        if (host.endsWith("ssgcdn.com")) {
            path = path.replaceFirst("_[0-9]{2,4}(?=\\.[^.]+$)", "_SIZE");
        }
        return host + path;
    }

    private List<String> mergedImages(
            ExtractedProductMetadata structured,
            ExtractedProductMetadata openGraph,
            ExtractedProductMetadata ai,
            String fallback
    ) {
        List<String> values = new ArrayList<>(mergedImages(structured, openGraph, ai));
        if (values.isEmpty() && fallback != null && !fallback.isBlank()) values.add(fallback);
        return values;
    }

    private long firstPositive(Long... values) {
        for (Long value : values) if (value != null && value > 0) return value;
        return 0;
    }

    private Long firstPositiveNullable(Long... values) {
        for (Long value : values) if (value != null && value > 0) return value;
        return null;
    }

    private Long firstNonNegative(Long... values) {
        for (Long value : values) if (value != null && value >= 0) return value;
        return null;
    }

    private String first(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return repairMojibake(value.strip());
        return "";
    }

    private String repairMojibake(String value) {
        if (!java.nio.charset.StandardCharsets.ISO_8859_1.newEncoder().canEncode(value)) return value;
        String decoded = new String(value.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1),
                java.nio.charset.StandardCharsets.UTF_8);
        return hangulCount(decoded) > hangulCount(value) ? decoded : value;
    }

    private long hangulCount(String value) {
        return value.codePoints().filter(codePoint -> codePoint >= 0xAC00 && codePoint <= 0xD7A3).count();
    }

    private boolean isProductOptionType(String value) {
        if (value == null || value.isBlank()) return false;
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return !(normalized.contains("사진") || normalized.contains("동영상")
                || normalized.contains("홍보") || normalized.contains("광고")
                || normalized.contains("욕설") || normalized.contains("비방")
                || normalized.contains("신고") || normalized.contains("리뷰")
                || normalized.contains("rtitle") || normalized.matches("^(전체|포토|동영상)\\(\\d+\\)$")
                || normalized.contains("추천순") || normalized.contains("최신순")
                || normalized.contains("평점높은순") || normalized.contains("평점낮은순"));
    }

    private boolean isProductOptionValue(String value) {
        if (value == null || value.isBlank()) return false;
        String normalized = value.toLowerCase(java.util.Locale.ROOT);
        return !(normalized.contains("shipping to") || normalized.contains("ship to")
                || normalized.contains("배송 국가") || normalized.contains("배송 지역")
                || normalized.contains("국가 선택") || normalized.contains("select country")
                || normalized.matches("^(전체|포토|동영상)\\(\\d+\\)$")
                || "0".equals(normalized));
    }

    private record RenderedOptionCatalog(
            List<ExtractedProductOption> options,
            List<ExtractedProductVariant> variants,
            Long price,
            Long originalPrice
    ) {
        private static RenderedOptionCatalog empty() {
            return new RenderedOptionCatalog(List.of(), List.of(), null, null);
        }
    }

    private String proxyImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) return "/assets/product-shoes.webp";
        imageSourceRegistry.register(imageUrl);
        return "/api/product-images/proxy?url=" + java.net.URLEncoder.encode(imageUrl, java.nio.charset.StandardCharsets.UTF_8);
    }

    private URI validatePublicHttpUrl(String value) throws Exception {
        URI uri = URI.create(value == null ? "" : value);
        if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme())) || uri.getHost() == null) {
            throw new IllegalArgumentException("지원하지 않는 상품 URL입니다.");
        }
        for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
            if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                    || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                throw new IllegalArgumentException("내부 네트워크 주소에는 접근할 수 없습니다.");
            }
        }
        return uri;
    }
}
