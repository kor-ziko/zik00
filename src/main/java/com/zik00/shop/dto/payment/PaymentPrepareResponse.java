package com.zik00.shop.dto.payment;

import java.util.List;

public record PaymentPrepareResponse(
        String paymentId,
        String orderName,
        long totalAmount,
        String currency,
        boolean paymentEnabled,
        String paymentProvider,
        List<PaymentMethodResponse> paymentMethods,
        long productAmount,
        long domesticShippingFee,
        long agencyFee,
        long estimatedShippingFee,
        long estimatedShippingMin,
        long estimatedShippingMax,
        long estimatedDuty,
        long estimatedConsumptionTax,
        long estimatedImportCharges,
        boolean customsFinalizationRequired,
        Address deliveryAddress,
        List<PaymentItemResponse> items
) {
    public record Address(
            long id,
            String addressName,
            String receiverName,
            String receiverPhone,
            String zipCode,
            String province,
            String detailAddress
    ) {}
}
