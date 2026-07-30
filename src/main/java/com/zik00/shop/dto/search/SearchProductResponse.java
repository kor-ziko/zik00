package com.zik00.shop.dto.search;

public record SearchProductResponse(
        String productId,
        String name,
        String category,
        String brand,
        long price,
        Long originalPrice,
        String currency,
        String sourceUrl,
        String imageUrl,
        double rating,
        int reviewCount,
        boolean freeShipping,
        String source,
        String badge
) {
}
