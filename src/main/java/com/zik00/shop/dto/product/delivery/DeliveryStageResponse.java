package com.zik00.shop.dto.product.delivery;

public record DeliveryStageResponse(
        String code,
        String label,
        int minimumDays,
        int maximumDays
) {
}
