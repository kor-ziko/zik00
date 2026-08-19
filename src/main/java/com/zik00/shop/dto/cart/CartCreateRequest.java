package com.zik00.shop.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.util.Map;

public record CartCreateRequest(
        @NotBlank @Size(max = 255) String productId,
        @NotBlank @Size(max = 500) String productName,
        @Size(max = 200) String brand,
        @Size(max = 1500) String imageUrl,
        @PositiveOrZero long unitPrice,
        @NotBlank @Size(max = 10) String currency,
        @Size(max = 2000) String sourceUrl,
        Map<String, String> selectedOptions,
        @Min(1) @Max(10) int quantity
) {
}
