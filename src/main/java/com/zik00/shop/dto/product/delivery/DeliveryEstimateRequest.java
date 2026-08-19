package com.zik00.shop.dto.product.delivery;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DeliveryEstimateRequest(
        @NotBlank @Size(max = 500) String productName,
        @NotBlank @Size(max = 500) String category,
        @Size(max = 2_000) String sourceUrl
) {
}
