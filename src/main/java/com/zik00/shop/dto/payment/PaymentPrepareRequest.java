package com.zik00.shop.dto.payment;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import java.util.List;

public record PaymentPrepareRequest(
        @NotEmpty List<@Positive Long> cartItemIds,
        @Positive long deliveryAddressId
) {
}
