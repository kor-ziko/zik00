package com.zik00.shop.dto.cart;

import java.util.List;

public record CartResponse(List<CartItemResponse> items, long itemCount) {
}
