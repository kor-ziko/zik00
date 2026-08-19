package com.zik00.shop.dto.payment;

import jakarta.validation.constraints.NotBlank;

public record PaymentStartRequest(
        @NotBlank String paymentId,
        @NotBlank String paymentMethod
) {
}
