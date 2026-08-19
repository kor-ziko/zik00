package com.zik00.shop.dto.payment;

import java.util.Map;

public record PaymentStartResponse(String requestUrl, Map<String, String> fields) {
}
