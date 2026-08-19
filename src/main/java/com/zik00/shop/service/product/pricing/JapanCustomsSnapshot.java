package com.zik00.shop.service.product.pricing;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;

public record JapanCustomsSnapshot(
        BigDecimal krwToJpyRate,
        LocalDate rateFrom,
        LocalDate rateTo,
        Map<String, BigDecimal> simplifiedTariffRates,
        BigDecimal consumptionTaxRate,
        Instant fetchedAt,
        boolean fallback
) {
    public JapanCustomsSnapshot(
            BigDecimal krwToJpyRate,
            LocalDate rateFrom,
            LocalDate rateTo,
            Map<String, BigDecimal> simplifiedTariffRates,
            Instant fetchedAt,
            boolean fallback
    ) {
        this(krwToJpyRate, rateFrom, rateTo, simplifiedTariffRates,
                new BigDecimal("0.10"), fetchedAt, fallback);
    }
}
