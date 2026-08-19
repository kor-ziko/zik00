package com.zik00.shop.service.product.url;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.dto.product.url.ProductUrlResolveResponse;
import com.zik00.shop.service.product.extractor.ProductPageExtractor;
import com.zik00.shop.service.search.ExternalProductCatalog;
import org.springframework.stereotype.Service;

import java.net.InetAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Map;

@Service
public class ProductUrlImportService {
    private final ProductPageExtractor productPageExtractor;
    private final ExternalProductCatalog externalProductCatalog;

    public ProductUrlImportService(
            ProductPageExtractor productPageExtractor,
            ExternalProductCatalog externalProductCatalog
    ) {
        this.productPageExtractor = productPageExtractor;
        this.externalProductCatalog = externalProductCatalog;
    }

    public ProductUrlResolveResponse resolve(String rawUrl) {
        URI uri = validateAndNormalize(rawUrl);
        String sourceUrl = uri.toString();
        String productId = "URL-" + sha256(sourceUrl).substring(0, 20);
        if (externalProductCatalog.findDetail(productId).isPresent()) {
            return new ProductUrlResolveResponse(productId);
        }

        DiscoveredProduct product = new DiscoveredProduct(
                productId, productId, null, uri.getHost(), "기타", "브랜드 정보 없음",
                0, null, "KRW", sourceUrl, "", null, 0,
                uri.getHost(), "", Map.of()
        );
        externalProductCatalog.saveProduct(product);

        ProductDetailResponse detail;
        try {
            detail = productPageExtractor.extract(product);
        } catch (RuntimeException exception) {
            throw new IllegalStateException("상품 페이지 정보를 가져오지 못했습니다.", exception);
        }
        if (!containsProductInformation(detail, uri.getHost())) {
            throw new IllegalStateException("이 판매 페이지에서는 상품 정보를 확인할 수 없습니다.");
        }
        externalProductCatalog.saveDetail(detail);
        return new ProductUrlResolveResponse(productId);
    }

    private URI validateAndNormalize(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.strip());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            if (!("http".equals(scheme) || "https".equals(scheme)) || uri.getHost() == null
                    || uri.getUserInfo() != null || (uri.getPort() != -1 && uri.getPort() != 80 && uri.getPort() != 443)) {
                throw new IllegalArgumentException("http 또는 https 상품 URL을 입력해 주세요.");
            }
            for (InetAddress address : InetAddress.getAllByName(uri.getHost())) {
                if (address.isAnyLocalAddress() || address.isLoopbackAddress() || address.isLinkLocalAddress()
                        || address.isSiteLocalAddress() || address.isMulticastAddress()) {
                    throw new IllegalArgumentException("내부 네트워크 주소는 사용할 수 없습니다.");
                }
            }
            String path = uri.getRawPath() == null || uri.getRawPath().isBlank() ? "/" : uri.getRawPath();
            return new URI(scheme, null, uri.getHost().toLowerCase(Locale.ROOT), uri.getPort(),
                    path, uri.getRawQuery(), null);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("확인할 수 없는 상품 URL입니다.", exception);
        }
    }

    private boolean containsProductInformation(ProductDetailResponse detail, String fallbackName) {
        String name = detail.name() == null ? "" : detail.name().strip();
        String normalizedName = name.toLowerCase(Locale.ROOT);
        boolean blocked = normalizedName.contains("access denied") || normalizedName.contains("forbidden")
                || normalizedName.contains("captcha") || normalizedName.contains("접근이 거부");
        boolean hasRealName = !name.isBlank() && !name.equalsIgnoreCase(fallbackName);
        boolean hasDescription = detail.description() != null && !detail.description().isBlank();
        return !blocked && (detail.price() > 0 || hasRealName || hasDescription);
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("상품 URL 식별자를 만들 수 없습니다.", exception);
        }
    }
}
