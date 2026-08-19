package com.zik00.shop.dto.product;

import java.util.List;

public record ProductDetailResponse(
        String id,
        String sourceUrl,
        String name,
        String category,
        long price,
        Long originalPrice,
        String image,
        List<String> images,
        String brand,
        String description,
        String currency,
        long domesticShippingFee,
        boolean domesticShippingFeeEstimated,
        Double rating,
        int reviewCount,
        List<ProductOptionResponse> options,
        List<ProductVariantResponse> variants,
        List<String> tags
) {
}
