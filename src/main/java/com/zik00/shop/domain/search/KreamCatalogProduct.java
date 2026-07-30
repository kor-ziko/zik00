package com.zik00.shop.domain.search;

import com.zik00.shop.domain.product.KreamCatalogReview;

import java.util.List;

public record KreamCatalogProduct(
        String productId,
        String sourceUrl,
        String name,
        String category,
        String brand,
        String description,
        long price,
        long discountedPrice,
        String currency,
        int discountRate,
        boolean available,
        String thumbnailUrl,
        List<String> images,
        Double rating,
        int reviewCount,
        List<KreamCatalogReview> reviews,
        List<String> tags
) {
}
