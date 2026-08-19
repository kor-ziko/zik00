package com.zik00.shop.service.payment;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JapanPaymentProperties {
    private final BigDecimal krwToJpyRate;

    public JapanPaymentProperties(
            @Value("${shop.payment.japan.krw-to-jpy-rate:0}") BigDecimal krwToJpyRate
    ) {
        this.krwToJpyRate = krwToJpyRate == null ? BigDecimal.ZERO : krwToJpyRate;
    }

    public long toJpy(long amount, String currency) {
        if ("JPY".equalsIgnoreCase(currency)) return amount;
        if (!"KRW".equalsIgnoreCase(currency) || krwToJpyRate.signum() <= 0) {
            throw new IllegalStateException("KRW→JPY 적용환율이 설정되지 않았습니다.");
        }
        return BigDecimal.valueOf(amount).multiply(krwToJpyRate)
                .setScale(0, RoundingMode.CEILING).longValueExact();
    }

    public BigDecimal krwToJpyRate() {
        return krwToJpyRate;
    }

}
