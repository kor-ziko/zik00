package com.zik00.shop.dto.cart;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record CartQuantityUpdateRequest(@Min(1) @Max(10) int quantity) {
}
