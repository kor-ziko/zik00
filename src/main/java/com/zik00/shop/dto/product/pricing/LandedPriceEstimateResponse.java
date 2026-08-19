package com.zik00.shop.dto.product.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record LandedPriceEstimateResponse(
        long sourceProductPrice,
        String sourceCurrency,
        BigDecimal operatingExchangeRate,
        long convertedProductPrice,
        long convertedLocalDistributionFee,
        long agencyFee,
        long payableNow,
        BigDecimal customsExchangeRate,
        LocalDate customsRateFrom,
        LocalDate customsRateTo,
        long customsValue,
        BigDecimal dutyRate,
        Long estimatedDuty,
        Long estimatedConsumptionTax,
        Long estimatedImportCharges,
        Long estimatedTotalCost,
        String customsStatus,
        boolean staleCustomsData,
        String internationalShippingStatus,
        int estimatedWeightMinGrams,
        int estimatedWeightMaxGrams,
        long estimatedInternationalShippingMin,
        long estimatedInternationalShippingMax,
        long estimatedInternationalShippingFee,
        Long estimatedTotalCostMin,
        Long estimatedTotalCostMax,
        String hsCodeCandidate,
        String customsClassificationMethod,
        String shippingEstimationBasis,
        List<String> notices
) {
}
