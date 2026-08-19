package com.zik00.shop.dto.cart;

import java.time.LocalDateTime;
import java.util.Map;

public record CartItemResponse(
        long id, String productId, String productName, String brand, String imageUrl,
        long unitPrice, String currency, String sourceUrl, Map<String, String> selectedOptions,
        int quantity, LocalDateTime createdAt
) {
}
