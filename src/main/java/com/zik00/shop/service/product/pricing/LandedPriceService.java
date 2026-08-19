package com.zik00.shop.service.product.pricing;

import com.zik00.shop.dto.product.pricing.LandedPriceEstimateRequest;
import com.zik00.shop.dto.product.pricing.LandedPriceEstimateResponse;
import com.zik00.shop.service.payment.OperatingExchangeRateService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LandedPriceService {
    private static final BigDecimal PERSONAL_IMPORT_FACTOR = new BigDecimal("0.60");
    private static final BigDecimal DEFAULT_CONSUMPTION_TAX_RATE = new BigDecimal("0.10");
    private static final long TAX_EXEMPT_CUSTOMS_VALUE = 10_000L;
    private static final long SIMPLIFIED_TARIFF_LIMIT = 200_000L;

    private final OperatingExchangeRateService exchangeRateService;
    private final JapanCustomsDataService customsDataService;
    private final InternationalShippingEstimator shippingEstimator;
    private final CustomsProductClassifier productClassifier;
    private final BigDecimal agencyFeeRate;

    @Autowired
    public LandedPriceService(
            OperatingExchangeRateService exchangeRateService,
            JapanCustomsDataService customsDataService,
            InternationalShippingEstimator shippingEstimator,
            CustomsProductClassifier productClassifier,
            @Value("${shop.pricing.agency-fee-rate:0}") BigDecimal agencyFeeRate
    ) {
        this.exchangeRateService = exchangeRateService;
        this.customsDataService = customsDataService;
        this.shippingEstimator = shippingEstimator;
        this.productClassifier = productClassifier;
        this.agencyFeeRate = agencyFeeRate == null ? BigDecimal.ZERO : agencyFeeRate.max(BigDecimal.ZERO);
    }

    LandedPriceService(
            OperatingExchangeRateService exchangeRateService,
            JapanCustomsDataService customsDataService,
            InternationalShippingEstimator shippingEstimator,
            BigDecimal agencyFeeRate
    ) {
        this(exchangeRateService, customsDataService, shippingEstimator, null, agencyFeeRate);
    }

    public LandedPriceEstimateResponse estimate(LandedPriceEstimateRequest request) {
        String currency = request.currency().trim().toUpperCase(Locale.ROOT);
        if (!List.of("KRW", "JPY").contains(currency)) throw bad("KRW 또는 JPY 상품만 계산할 수 있습니다.");

        long sourceProductPrice = Math.multiplyExact(request.unitPrice(), request.quantity());
        long convertedProductPrice = operatingJpy(sourceProductPrice, currency);
        long convertedLocalFee = operatingJpy(request.localDistributionFee(), currency);
        long agencyFee = ceil(BigDecimal.valueOf(convertedProductPrice).multiply(agencyFeeRate));
        InternationalShippingEstimator.Estimate shipping = shippingEstimator.estimate(
                request.productName(), request.category(), request.quantity()
        );
        long estimatedShippingFee = midpoint(shipping.minimumPriceJpy(), shipping.maximumPriceJpy());
        long serviceAmount = Math.addExact(Math.addExact(convertedProductPrice, convertedLocalFee), agencyFee);
        long payableNow = Math.addExact(serviceAmount, estimatedShippingFee);

        JapanCustomsDataService.SnapshotView view = customsDataService.current();
        JapanCustomsSnapshot snapshot = view.snapshot();
        List<String> notices = new ArrayList<>();
        notices.add("예상 국제배송비는 최초 결제에 포함되며 입고 후 실측 차액은 추가 청구 또는 환불됩니다.");
        notices.add("예상 국제배송비에는 관세와 일본 소비세가 포함되지 않으며 별도 항목으로 계산됩니다.");
        notices.add("예상 관부가세는 개인사용 목적 수입 기준이며 실제 세관 판정과 다를 수 있습니다.");
        CustomsProductClassifier.Classification classification = productClassifier == null
                ? CustomsProductClassifier.rules(request.productName(), request.category())
                : productClassifier.classify(request.productName(), request.category());

        if (snapshot.krwToJpyRate() == null || snapshot.krwToJpyRate().signum() <= 0) {
            notices.add("일본 세관 고시환율을 아직 확보하지 못해 관부가세를 계산하지 않았습니다.");
            return response(sourceProductPrice, currency, convertedProductPrice, convertedLocalFee,
                    agencyFee, payableNow, snapshot, view.stale(), 0, null, null, null,
                    null, null, "RATE_UNAVAILABLE", shipping, classification, notices);
        }

        long customsRetailPrice = customsJpy(sourceProductPrice, currency, snapshot.krwToJpyRate());
        long customsValue = floor(BigDecimal.valueOf(customsRetailPrice).multiply(PERSONAL_IMPORT_FACTOR));
        boolean exemptionExcluded = classification.smallValueExemptionExcluded();

        if (customsValue <= TAX_EXEMPT_CUSTOMS_VALUE && !exemptionExcluded) {
            long total = serviceAmount;
            return response(sourceProductPrice, currency, convertedProductPrice, convertedLocalFee,
                    agencyFee, payableNow, snapshot, view.stale(), customsValue, BigDecimal.ZERO,
                    0L, 0L, 0L, total, "EXEMPT_ESTIMATE", shipping, classification, notices);
        }

        if (customsValue > SIMPLIFIED_TARIFF_LIMIT || classification.generalTariffRequired()) {
            BigDecimal estimatedRate = generalDutyRate(request, classification, snapshot);
            long duty = generalDuty(request, classification, customsValue, estimatedRate);
            long consumptionTax = ceil(
                    BigDecimal.valueOf(Math.addExact(customsValue, duty)).multiply(consumptionTaxRate(snapshot))
            );
            long importCharges = Math.addExact(duty, consumptionTax);
            long total = Math.addExact(serviceAmount, importCharges);
            long payableWithEstimatedImportCharges = Math.addExact(payableNow, importCharges);
            notices.add("HS 코드 후보와 일본 세관 주요 품목 세율을 이용한 예상 관부가세를 최초 결제에 포함했습니다.");
            notices.add("재질·용도·원산지 및 세관 판정에 따른 실제 차액은 통관 후 추가 청구 또는 환불됩니다.");
            return response(sourceProductPrice, currency, convertedProductPrice, convertedLocalFee,
                    agencyFee, payableWithEstimatedImportCharges, snapshot, view.stale(), customsValue,
                    estimatedRate, duty, consumptionTax, importCharges, total,
                    "GENERAL_TARIFF_ESTIMATED", shipping, classification, notices);
        }

        BigDecimal dutyRate = snapshot.simplifiedTariffRates().getOrDefault(
                classification.simplifiedTariffGroup(), new BigDecimal("0.05")
        );
        long duty = ceil(BigDecimal.valueOf(customsValue).multiply(dutyRate));
        long consumptionTax = ceil(
                BigDecimal.valueOf(Math.addExact(customsValue, duty)).multiply(consumptionTaxRate(snapshot))
        );
        long importCharges = Math.addExact(duty, consumptionTax);
        long total = Math.addExact(serviceAmount, importCharges);
        long payableWithEstimatedImportCharges = Math.addExact(payableNow, importCharges);
        notices.add("계산된 예상 관부가세는 지금 결제 예정 금액에 포함되어 있습니다.");
        return response(sourceProductPrice, currency, convertedProductPrice, convertedLocalFee,
                agencyFee, payableWithEstimatedImportCharges, snapshot, view.stale(), customsValue, dutyRate, duty,
                consumptionTax, importCharges, total, "ESTIMATED", shipping, classification, notices);
    }

    private long operatingJpy(long amount, String currency) {
        try {
            return exchangeRateService.toJpy(amount, currency);
        } catch (IllegalStateException exception) {
            throw bad(exception.getMessage());
        }
    }

    private long customsJpy(long amount, String currency, BigDecimal customsRate) {
        if ("JPY".equals(currency)) return amount;
        return ceil(BigDecimal.valueOf(amount).multiply(customsRate));
    }

    private long ceil(BigDecimal value) {
        return value.setScale(0, RoundingMode.CEILING).longValueExact();
    }

    private long floor(BigDecimal value) {
        return value.setScale(0, RoundingMode.FLOOR).longValueExact();
    }

    private BigDecimal consumptionTaxRate(JapanCustomsSnapshot snapshot) {
        return snapshot.consumptionTaxRate() == null
                ? DEFAULT_CONSUMPTION_TAX_RATE : snapshot.consumptionTaxRate();
    }

    private BigDecimal generalDutyRate(
            LandedPriceEstimateRequest request,
            CustomsProductClassifier.Classification classification,
            JapanCustomsSnapshot snapshot
    ) {
        String text = (request.productName() + " " + request.category()).toLowerCase(Locale.ROOT);
        String key;
        if (classification.hsCodeCandidate().startsWith("6110") || contains(text, "니트", "스웨터", "가디건")) {
            key = "GENERAL:KNIT";
        } else if (contains(text, "신발", "슈즈", "부츠", "샌들", "슬리퍼", "로퍼", "스니커즈", "구두")) {
            key = "GENERAL:FOOTWEAR";
        } else if (contains(text, "가방", "핸드백")) {
            key = "GENERAL:HANDBAG";
        } else if (contains(text, "주얼리", "쥬얼리", "귀금속")) {
            key = "GENERAL:JEWELRY";
        } else {
            key = "GENERAL:APPAREL";
        }
        return snapshot.simplifiedTariffRates().getOrDefault(key, new BigDecimal("0.109"));
    }

    private long generalDuty(
            LandedPriceEstimateRequest request,
            CustomsProductClassifier.Classification classification,
            long customsValue,
            BigDecimal rate
    ) {
        long percentageDuty = ceil(BigDecimal.valueOf(customsValue).multiply(rate));
        String text = (request.productName() + " " + request.category()).toLowerCase(Locale.ROOT);
        boolean leatherFootwear = contains(text, "가죽", "leather")
                && contains(text, "신발", "슈즈", "부츠", "샌들", "슬리퍼", "로퍼", "스니커즈", "구두", "footwear", "shoes");
        if (!leatherFootwear) return percentageDuty;
        return Math.max(percentageDuty, Math.multiplyExact(4_300L, request.quantity()));
    }

    private boolean contains(String text, String... candidates) {
        for (String candidate : candidates) if (text.contains(candidate)) return true;
        return false;
    }

    private long midpoint(long minimum, long maximum) {
        return BigDecimal.valueOf(minimum).add(BigDecimal.valueOf(maximum))
                .divide(new BigDecimal("2"), 0, RoundingMode.CEILING).longValueExact();
    }

    private LandedPriceEstimateResponse response(
            long sourceProductPrice,
            String currency,
            long convertedProductPrice,
            long convertedLocalFee,
            long agencyFee,
            long payableNow,
            JapanCustomsSnapshot snapshot,
            boolean stale,
            long customsValue,
            BigDecimal dutyRate,
            Long duty,
            Long consumptionTax,
            Long importCharges,
            Long total,
            String status,
            InternationalShippingEstimator.Estimate shipping,
            CustomsProductClassifier.Classification classification,
            List<String> notices
    ) {
        Long totalMin = total == null ? null : Math.addExact(total, shipping.minimumPriceJpy());
        Long totalMax = total == null ? null : Math.addExact(total, shipping.maximumPriceJpy());
        return new LandedPriceEstimateResponse(
                sourceProductPrice, currency, exchangeRateService.currentRate(), convertedProductPrice,
                convertedLocalFee, agencyFee, payableNow, snapshot.krwToJpyRate(), snapshot.rateFrom(),
                snapshot.rateTo(), customsValue, dutyRate, duty, consumptionTax, importCharges, total,
                status, stale, "예상 범위 · 입고 후 실측 확정",
                shipping.minimumWeightGrams(), shipping.maximumWeightGrams(),
                shipping.minimumPriceJpy(), shipping.maximumPriceJpy(),
                midpoint(shipping.minimumPriceJpy(), shipping.maximumPriceJpy()), totalMin, totalMax,
                classification.hsCodeCandidate(), classification.method(),
                shipping.basis(), List.copyOf(notices)
        );
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
