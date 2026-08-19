package com.zik00.shop.service.product.extractor;

import java.util.Map;

public record ExtractedProductVariant(
        String variantId,
        Map<String, String> attributes,
        Long price,
        Boolean available
) {
}
