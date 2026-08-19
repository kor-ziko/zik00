package com.zik00.shop.service.product.pricing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zik00.shop.dto.product.pricing.LandedPriceEstimateRequest;
import com.zik00.shop.dto.product.pricing.LandedPriceEstimateResponse;
import com.zik00.shop.service.payment.OperatingExchangeRateService;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LandedPriceServiceTest {
    private LandedPriceService service;

    @BeforeEach
    void setUp() {
        OperatingExchangeRateService exchangeRateService = mock(OperatingExchangeRateService.class);
        when(exchangeRateService.currentRate()).thenReturn(new BigDecimal("0.11"));
        when(exchangeRateService.toJpy(org.mockito.ArgumentMatchers.anyLong(), org.mockito.ArgumentMatchers.anyString()))
                .thenAnswer(invocation -> {
                    long amount = invocation.getArgument(0);
                    String currency = invocation.getArgument(1);
                    if ("JPY".equalsIgnoreCase(currency)) return amount;
                    return BigDecimal.valueOf(amount).multiply(new BigDecimal("0.11"))
                            .setScale(0, java.math.RoundingMode.CEILING).longValueExact();
                });

        JapanCustomsDataService customsDataService = mock(JapanCustomsDataService.class);
        JapanCustomsSnapshot snapshot = new JapanCustomsSnapshot(
                new BigDecimal("0.10"),
                LocalDate.of(2026, 8, 16),
                LocalDate.of(2026, 8, 22),
                Map.of(
                        "2", new BigDecimal("0.20"),
                        "3", new BigDecimal("0.15"),
                        "4", new BigDecimal("0.10"),
                        "5", new BigDecimal("0.03"),
                        "6", BigDecimal.ZERO,
                        "7", new BigDecimal("0.05")
                ),
                Instant.now(),
                false
        );
        when(customsDataService.current())
                .thenReturn(new JapanCustomsDataService.SnapshotView(snapshot, false));
        InternationalShippingEstimator shippingEstimator = mock(InternationalShippingEstimator.class);
        when(shippingEstimator.estimate(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyInt()
        )).thenReturn(new InternationalShippingEstimator.Estimate(
                500, 1_000, 2_585, 2_860, "테스트 배송비"
        ));
        service = new LandedPriceService(
                exchangeRateService, customsDataService, shippingEstimator, BigDecimal.ZERO
        );
    }

    @Test
    void exemptsOrdinaryPersonalImportWhenCustomsValueIsAtMostTenThousandYen() {
        LandedPriceEstimateResponse result = service.estimate(request("스마트폰 케이스", "가전", 100_000));

        assertThat(result.customsValue()).isEqualTo(6_000);
        assertThat(result.customsStatus()).isEqualTo("EXEMPT_ESTIMATE");
        assertThat(result.estimatedImportCharges()).isZero();
        assertThat(result.payableNow()).isEqualTo(13_723);
        assertThat(result.estimatedInternationalShippingMin()).isEqualTo(2_585);
        assertThat(result.estimatedInternationalShippingFee()).isEqualTo(2_723);
        assertThat(result.estimatedTotalCostMin()).isEqualTo(13_585);
    }

    @Test
    void estimatesGeneralTariffForFootwearAndMarksItForLaterReconciliation() {
        LandedPriceEstimateResponse result = service.estimate(request("가죽 스니커즈", "패션의류 > 신발", 200_000));

        assertThat(result.customsValue()).isEqualTo(12_000);
        assertThat(result.customsStatus()).isEqualTo("GENERAL_TARIFF_ESTIMATED");
        assertThat(result.estimatedDuty()).isEqualTo(4_300);
        assertThat(result.estimatedConsumptionTax()).isEqualTo(1_630);
        assertThat(result.estimatedImportCharges()).isEqualTo(5_930);
        assertThat(result.payableNow()).isEqualTo(30_653);
    }

    @Test
    void estimatesSimplifiedDutyAndConsumptionTaxForOrdinaryGoods() {
        LandedPriceEstimateResponse result = service.estimate(request("무선 마우스", "가전", 300_000));

        assertThat(result.customsValue()).isEqualTo(18_000);
        assertThat(result.dutyRate()).isEqualByComparingTo("0.05");
        assertThat(result.estimatedDuty()).isEqualTo(900);
        assertThat(result.estimatedConsumptionTax()).isEqualTo(1_890);
        assertThat(result.estimatedImportCharges()).isEqualTo(2_790);
        assertThat(result.payableNow()).isEqualTo(38_513);
        assertThat(result.customsStatus()).isEqualTo("ESTIMATED");
    }

    private LandedPriceEstimateRequest request(String name, String category, long unitPrice) {
        return new LandedPriceEstimateRequest(name, category, unitPrice, "KRW", 1, 0);
    }
}
