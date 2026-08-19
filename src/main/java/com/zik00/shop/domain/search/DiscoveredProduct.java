package com.zik00.shop.domain.search;

import java.util.List;
import java.util.Map;

public record DiscoveredProduct(
        String productId,
        String providerProductId,
        String immersivePageToken,
        String name,
        String category,
        String brand,
        long price,
        Long originalPrice,
        String currency,
        String sourceUrl,
        String imageUrl,
        Double rating,
        int reviewCount,
        String source,
        String description,
        Map<String, List<String>> options
) {
    public DiscoveredProduct withMerchant(String merchantName, String merchantUrl, long merchantPrice, Long merchantOriginalPrice) {
        return new DiscoveredProduct(
                productId, providerProductId, immersivePageToken, name, category, brand,
                merchantPrice > 0 ? merchantPrice : price,
                merchantOriginalPrice != null ? merchantOriginalPrice : originalPrice,
                currency, merchantUrl, imageUrl, rating, reviewCount,
                merchantName == null || merchantName.isBlank() ? source : merchantName,
                description, options
        );
    }

    public DiscoveredProduct withOptions(Map<String, List<String>> resolvedOptions) {
        return new DiscoveredProduct(
                productId, providerProductId, immersivePageToken, name, category, brand,
                price, originalPrice, currency, sourceUrl, imageUrl, rating, reviewCount,
                source, description, resolvedOptions
        );
    }
}
