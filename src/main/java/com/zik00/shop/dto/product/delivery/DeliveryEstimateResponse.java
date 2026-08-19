package com.zik00.shop.dto.product.delivery;

import java.util.List;

public record DeliveryEstimateResponse(
        int minimumDays,
        int maximumDays,
        List<DeliveryStageResponse> stages,
        String basis
) {
}
