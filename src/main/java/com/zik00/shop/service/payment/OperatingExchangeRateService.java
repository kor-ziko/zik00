package com.zik00.shop.service.payment;

import com.zik00.shop.service.product.pricing.JapanCustomsDataService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OperatingExchangeRateService {
    private final JapanPaymentProperties paymentProperties;
    private final JapanCustomsDataService customsDataService;
    private final BigDecimal automaticMarkup;

    public OperatingExchangeRateService(
            JapanPaymentProperties paymentProperties,
            JapanCustomsDataService customsDataService,
            @Value("${shop.payment.japan.automatic-rate-markup:1.03}") BigDecimal automaticMarkup
    ) {
        this.paymentProperties = paymentProperties;
        this.customsDataService = customsDataService;
        this.automaticMarkup = automaticMarkup == null
                ? BigDecimal.ONE
                : automaticMarkup.max(BigDecimal.ONE);
    }

    public BigDecimal currentRate() {
        if (paymentProperties.krwToJpyRate().signum() > 0) return paymentProperties.krwToJpyRate();
        BigDecimal customsRate = customsDataService.current().snapshot().krwToJpyRate();
        if (customsRate == null || customsRate.signum() <= 0) {
            throw new IllegalStateException("원화→엔화 환율을 아직 가져오지 못했습니다. 잠시 후 다시 시도해 주세요.");
        }
        return customsRate.multiply(automaticMarkup).setScale(6, RoundingMode.HALF_UP);
    }

    public long toJpy(long amount, String currency) {
        if ("JPY".equalsIgnoreCase(currency)) return amount;
        if (!"KRW".equalsIgnoreCase(currency)) {
            throw new IllegalStateException("KRW 또는 JPY 상품만 환산할 수 있습니다.");
        }
        return BigDecimal.valueOf(amount).multiply(currentRate())
                .setScale(0, RoundingMode.CEILING).longValueExact();
    }
}
