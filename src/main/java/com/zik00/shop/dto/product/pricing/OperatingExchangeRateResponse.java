package com.zik00.shop.dto.product.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

public record OperatingExchangeRateResponse(
        String sourceCurrency,
        String targetCurrency,
        BigDecimal rate,
        LocalDate customsRateFrom,
        LocalDate customsRateTo,
        boolean stale
) {
}
