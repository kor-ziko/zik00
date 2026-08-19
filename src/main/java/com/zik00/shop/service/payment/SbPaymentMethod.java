package com.zik00.shop.service.payment;

import java.util.Arrays;
import java.util.Optional;

public enum SbPaymentMethod {
    CREDIT_CARD("credit3d2", "신용·체크카드", "Visa, Mastercard, JCB 등"),
    PAYPAY("paypay", "PayPay", "PayPay 앱 또는 계정으로 결제"),
    PAYPAL("paypal", "PayPal", "PayPal 계정으로 결제");

    private final String code;
    private final String label;
    private final String description;

    SbPaymentMethod(String code, String label, String description) {
        this.code = code;
        this.label = label;
        this.description = description;
    }

    public String code() { return code; }
    public String label() { return label; }
    public String description() { return description; }

    public static Optional<SbPaymentMethod> fromCode(String code) {
        if (code == null) return Optional.empty();
        return Arrays.stream(values()).filter(method -> method.code.equalsIgnoreCase(code.trim())).findFirst();
    }
}
