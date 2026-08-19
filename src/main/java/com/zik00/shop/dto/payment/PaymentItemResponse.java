package com.zik00.shop.dto.payment;

import java.util.Map;

public record PaymentItemResponse(
        long cartItemId,
        String productId,
        String productName,
        String brand,
        String imageUrl,
        long unitPrice,
        int quantity,
        long subtotal,
        Map<String, String> selectedOptions
) {
}
