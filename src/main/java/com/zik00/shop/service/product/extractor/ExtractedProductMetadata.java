package com.zik00.shop.service.product.extractor;

import java.util.List;

public record ExtractedProductMetadata(
        String name,
        String brand,
        String description,
        Long price,
        Long originalPrice,
        String currency,
        Long domesticShippingFee,
        String image,
        List<String> images,
        List<ExtractedProductOption> options,
        List<ExtractedProductVariant> variants
) {
    public static ExtractedProductMetadata empty() {
        return new ExtractedProductMetadata("", "", "", null, null, "", null, "", List.of(), List.of(), List.of());
    }
}
