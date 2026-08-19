package com.zik00.shop.dto.product;

import java.util.List;

public record ProductOptionResponse(String optionType, List<String> values) {
}
