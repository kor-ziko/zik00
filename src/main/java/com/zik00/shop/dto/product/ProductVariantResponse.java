package com.zik00.shop.dto.product;

import java.util.Map;

public record ProductVariantResponse(
        String variantId,
        Map<String, String> attributes,
        Long price,
        boolean available
) {
}
