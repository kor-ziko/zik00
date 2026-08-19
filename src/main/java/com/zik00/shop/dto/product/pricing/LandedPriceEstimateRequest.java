package com.zik00.shop.dto.product.pricing;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record LandedPriceEstimateRequest(
        @NotBlank @Size(max = 500) String productName,
        @NotBlank @Size(max = 300) String category,
        @PositiveOrZero long unitPrice,
        @NotBlank @Size(max = 10) String currency,
        @Min(1) @Max(10) int quantity,
        @PositiveOrZero long localDistributionFee
) {
}
