package com.zik00.shop.service.search.provider;

import com.zik00.shop.domain.search.DiscoveredProduct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Component
public class SerpApiProductSearchProvider implements ProductSearchProvider {
    private static final Logger log = LoggerFactory.getLogger(SerpApiProductSearchProvider.class);
    private static final String SEARCH_ENDPOINT = "https://serpapi.com/search.json";

    private final boolean enabled;
    private final String apiKey;
    private final SerpApiQuotaGuard quotaGuard;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SerpApiProductSearchProvider(
            @Value("${shop.product-discovery.provider:disabled}") String provider,
            @Value("${shop.product-discovery.serpapi-key:}") String apiKey,
            SerpApiQuotaGuard quotaGuard
    ) {
        this.enabled = "serpapi".equalsIgnoreCase(provider) && !apiKey.isBlank();
        this.apiKey = apiKey;
        this.quotaGuard = quotaGuard;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(8)).build();
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public List<DiscoveredProduct> search(String query, String category) {
        String searchQuery = buildSearchQuery(query, category);
        if (!enabled || searchQuery.isBlank() || !quotaGuard.tryAcquire()) return List.of();
        try {
            JsonNode root = request(SEARCH_ENDPOINT
                    + "?engine=google_shopping&gl=kr&hl=ko&location=South%20Korea&q=" + encode(searchQuery)
                    + "&direct_link=true"
                    + "&api_key=" + encode(apiKey));
            JsonNode results = root.path("shopping_results");
            if (!results.isArray()) results = root.path("inline_shopping_results");
            if (!results.isArray()) return List.of();

            List<DiscoveredProduct> products = new ArrayList<>();
            for (JsonNode item : results) {
                String title = item.path("title").asString().strip();
                if (title.isBlank()) continue;
                String providerId = item.path("product_id").asString();
                String token = item.path("immersive_product_page_token").asString();
                String source = item.path("source").asString("Google Shopping");
                String sourceUrl = merchantUrl(
                        item.path("direct_link").asString(),
                        item.path("link").asString()
                );
                if (sourceUrl.isBlank()) sourceUrl = item.path("product_link").asString();
                String identity = firstNotBlank(providerId, token, sourceUrl, title + "|" + source);
                long price = Math.round(item.path("extracted_price").asDouble(0));
                Long originalPrice = item.path("extracted_old_price").isNumber()
                        ? Math.round(item.path("extracted_old_price").asDouble()) : null;
                String currency = currency(item);
                products.add(new DiscoveredProduct(
                        "SERP-" + sha256(identity).substring(0, 20),
                        providerId,
                        token,
                        title,
                        category == null || category.isBlank() ? "통합 카테고리" : category,
                        "브랜드 정보 없음",
                        price,
                        originalPrice,
                        currency,
                        sourceUrl,
                        firstNotBlank(item.path("thumbnail").asString(), item.path("serpapi_thumbnail").asString()),
                        item.path("rating").isNumber() ? item.path("rating").asDouble() : null,
                        item.path("reviews").asInt(0),
                        source,
                        item.path("snippet").asString(),
                        Map.of()
                ));
            }
            return products;
        } catch (Exception exception) {
            log.warn("SerpApi 상품 검색에 실패했습니다: {}", query, exception);
            return List.of();
        }
    }

    private String buildSearchQuery(String query, String category) {
        String normalizedQuery = query == null ? "" : query.strip();
        String normalizedCategory = category == null ? "" : category.strip().replace(" > ", " ");
        String combined;
        if (normalizedCategory.isBlank()) combined = normalizedQuery;
        else if (normalizedQuery.isBlank()) combined = normalizedCategory;
        else combined = normalizedCategory + " " + normalizedQuery;
        if (combined.isBlank() || combined.matches(".*(?:상품|쇼핑|구매).*")) return combined;
        return combined + " 상품";
    }

    @Override
    public DiscoveredProduct resolveMerchant(DiscoveredProduct product) {
        if (!enabled || product.immersivePageToken() == null || product.immersivePageToken().isBlank()) return product;
        if (!quotaGuard.tryAcquire()) return product;
        try {
            JsonNode root = request(SEARCH_ENDPOINT
                    + "?engine=google_immersive_product&page_token=" + encode(product.immersivePageToken())
                    + "&gl=kr&hl=ko&api_key=" + encode(apiKey));
            JsonNode result = root.path("product_results");
            DiscoveredProduct resolved = product.withOptions(variations(result));
            JsonNode stores = result.path("stores");
            if (!stores.isArray() || stores.isEmpty() || isDirectMerchantUrl(product.sourceUrl())) return resolved;
            JsonNode store = matchingStore(stores, product.source());
            if (store == null) return resolved;
            String link = merchantUrl(store.path("direct_link").asString(), store.path("link").asString());
            if (link.isBlank()) return resolved;
            long price = Math.round(store.path("extracted_price").asDouble(product.price()));
            Long originalPrice = store.path("extracted_original_price").isNumber()
                    ? Long.valueOf(Math.round(store.path("extracted_original_price").asDouble()))
                    : product.originalPrice();
            return resolved.withMerchant(store.path("name").asString(product.source()), link, price, originalPrice);
        } catch (Exception exception) {
            log.warn("SerpApi 상품 판매처 확인에 실패했습니다: {}", product.productId(), exception);
            return product;
        }
    }

    private JsonNode matchingStore(JsonNode stores, String expectedSource) {
        String normalizedSource = normalizeStoreName(expectedSource);
        for (JsonNode store : stores) {
            String link = merchantUrl(store.path("direct_link").asString(), store.path("link").asString());
            if (link.isBlank()) continue;
            String storeName = normalizeStoreName(store.path("name").asString());
            if (!normalizedSource.isBlank()
                    && (storeName.contains(normalizedSource) || normalizedSource.contains(storeName))) {
                return store;
            }
        }
        for (JsonNode store : stores) {
            if (!merchantUrl(store.path("direct_link").asString(), store.path("link").asString()).isBlank()) {
                return store;
            }
        }
        return null;
    }

    private String merchantUrl(String... candidates) {
        for (String candidate : candidates) {
            if (isDirectMerchantUrl(candidate)) return candidate;
        }
        return "";
    }

    private String normalizeStoreName(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{N}]", "");
    }

    private Map<String, List<String>> variations(JsonNode productResult) {
        Map<String, List<String>> options = new LinkedHashMap<>();
        JsonNode variations = productResult.path("variations");
        if (variations.isObject()) {
            variations.properties().forEach(entry -> {
                List<String> values = new ArrayList<>();
                if (entry.getValue().isArray()) {
                    entry.getValue().forEach(item -> {
                        String value = item.path("name").asString().strip();
                        if (!value.isBlank() && !values.contains(value)) values.add(value);
                    });
                }
                if (!values.isEmpty()) options.put(entry.getKey(), List.copyOf(values));
            });
        }
        JsonNode sizes = productResult.path("sizes");
        if (options.isEmpty() && sizes.isObject()) {
            List<String> values = new ArrayList<>();
            sizes.properties().forEach(entry -> values.add(entry.getKey()));
            if (!values.isEmpty()) options.put("사이즈", List.copyOf(values));
        }
        return Map.copyOf(options);
    }

    private JsonNode request(String url) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofSeconds(25))
                .header("Accept", "application/json")
                .GET()
                .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("SerpApi 응답 코드: " + response.statusCode());
        }
        JsonNode root = objectMapper.readTree(response.body());
        if (!root.path("error").asString().isBlank()) {
            throw new IllegalStateException(root.path("error").asString());
        }
        return root;
    }

    private boolean isDirectMerchantUrl(String value) {
        if (value == null || value.isBlank()) return false;
        try {
            String host = URI.create(value).getHost();
            return host != null && !host.endsWith("google.com") && !host.endsWith("google.co.kr");
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String currency(JsonNode item) {
        String alternate = item.path("alternative_price").path("currency").asString();
        if (!alternate.isBlank()) return alternate.toUpperCase(Locale.ROOT);
        String displayPrice = item.path("price").asString();
        return displayPrice.contains("¥") || displayPrice.contains("￥") ? "JPY" : "KRW";
    }

    private String firstNotBlank(String... values) {
        for (String value : values) if (value != null && !value.isBlank()) return value;
        return "";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
