package com.zik00.shop.service.payment;

import com.zik00.shop.dto.payment.PaymentStartResponse;
import com.zik00.shop.dto.payment.PaymentMethodResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class SbPaymentGateway {
    private static final DateTimeFormatter REQUEST_DATE = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    private static final List<String> REQUEST_ORDER = List.of(
            "pay_method", "merchant_id", "service_id", "cust_code", "sps_cust_no", "sps_payment_no",
            "order_id", "item_id", "pay_item_id", "item_name", "tax", "amount", "pay_type",
            "auto_charge_type", "service_type", "div_settele", "last_charge_month", "camp_type",
            "tracking_id", "terminal_type", "success_url", "cancel_url", "error_url", "pagecon_url",
            "free1", "free2", "free3", "free_csv", "request_date", "limit_second"
    );
    private static final List<String> RESPONSE_ORDER = List.of(
            "pay_method", "merchant_id", "service_id", "cust_code", "sps_cust_no", "sps_payment_no",
            "order_id", "item_id", "pay_item_id", "item_name", "tax", "amount", "pay_type",
            "auto_charge_type", "service_type", "div_settele", "last_charge_month", "camp_type",
            "tracking_id", "terminal_type", "free1", "free2", "free3", "request_date", "res_pay_method",
            "res_result", "res_tracking_id", "res_sps_cust_no", "res_sps_payment_no", "res_payinfo_key",
            "res_payment_date", "res_err_code", "res_date", "limit_second"
    );

    private final SbPaymentProperties properties;

    public SbPaymentGateway(SbPaymentProperties properties) {
        this.properties = properties;
    }

    public boolean configured() {
        return properties.configured();
    }

    public List<PaymentMethodResponse> paymentMethods() {
        return properties.paymentMethods().stream()
                .map(method -> new PaymentMethodResponse(
                        method.code(), method.label(), method.description()
                ))
                .toList();
    }

    public PaymentStartResponse createRequest(
            PendingPaymentStore.PendingPayment payment,
            String requestedPaymentMethod
    ) {
        if (!configured()) throw bad("SBPS 결제 설정이 완료되지 않았습니다.");
        if (payment.totalAmount() > 9_999_999L) throw bad("SBPS에서 한 번에 결제할 수 있는 금액을 초과했습니다.");
        SbPaymentMethod paymentMethod = SbPaymentMethod.fromCode(requestedPaymentMethod)
                .filter(properties.paymentMethods()::contains)
                .orElseThrow(() -> bad("사용할 수 없는 결제수단입니다."));

        LinkedHashMap<String, String> fields = new LinkedHashMap<>();
        put(fields, "pay_method", paymentMethod.code());
        put(fields, "merchant_id", properties.merchantId());
        put(fields, "service_id", properties.serviceId());
        put(fields, "cust_code", "member" + payment.memberId());
        put(fields, "sps_cust_no", "");
        put(fields, "sps_payment_no", "");
        put(fields, "order_id", limited(payment.paymentId(), 38));
        put(fields, "item_id", limited(firstProductId(payment), 32));
        put(fields, "pay_item_id", "");
        put(fields, "item_name", "ZIK00 ORDER");
        put(fields, "tax", "");
        put(fields, "amount", String.valueOf(payment.totalAmount()));
        put(fields, "pay_type", "0");
        put(fields, "auto_charge_type", "");
        put(fields, "service_type", "0");
        put(fields, "div_settele", "");
        put(fields, "last_charge_month", "");
        put(fields, "camp_type", "");
        put(fields, "tracking_id", "");
        put(fields, "terminal_type", "0");
        put(fields, "success_url", endpoint("/api/payment/sbps/return/success"));
        put(fields, "cancel_url", endpoint("/api/payment/sbps/return/cancel"));
        put(fields, "error_url", endpoint("/api/payment/sbps/return/error"));
        put(fields, "pagecon_url", endpoint("/api/payment/sbps/callback"));
        put(fields, "free1", "");
        put(fields, "free2", "");
        put(fields, "free3", "");
        put(fields, "free_csv", "");
        put(fields, "request_date", LocalDateTime.now().format(REQUEST_DATE));
        put(fields, "limit_second", "600");
        fields.put("sps_hashcode", SbPaymentSignature.request(fields, REQUEST_ORDER, properties.hashKey()));
        return new PaymentStartResponse(properties.requestUrl(), fields);
    }

    public Result verify(Map<String, String> fields) {
        if (!configured()) throw bad("SBPS 결제 설정이 완료되지 않았습니다.");
        if (!properties.merchantId().equals(fields.get("merchant_id"))
                || !properties.serviceId().equals(fields.get("service_id"))) {
            throw bad("SBPS 상점 정보가 일치하지 않습니다.");
        }
        if (!SbPaymentSignature.responseMatches(
                fields, RESPONSE_ORDER, properties.hashKey(), fields.get("sps_hashcode"))) {
            throw bad("SBPS 결제 결과의 서명이 올바르지 않습니다.");
        }
        long amount;
        try {
            amount = Long.parseLong(fields.getOrDefault("amount", ""));
        } catch (NumberFormatException exception) {
            throw bad("SBPS 결제 금액이 올바르지 않습니다.");
        }
        return new Result(
                fields.getOrDefault("order_id", ""), amount,
                fields.getOrDefault("res_pay_method", fields.getOrDefault("pay_method", "")),
                fields.getOrDefault("res_result", ""), fields.getOrDefault("res_payinfo_key", ""),
                fields.getOrDefault("res_err_code", "")
        );
    }

    public String clientResultUrl(String paymentId, PaymentState state, String message) {
        return properties.clientBaseUrl() + "/checkout/complete?paymentId=" + url(paymentId)
                + "&status=" + state.name().toLowerCase(Locale.ROOT)
                + (message == null || message.isBlank() ? "" : "&message=" + url(message));
    }

    private String endpoint(String path) {
        return properties.callbackBaseUrl() + path;
    }

    private String firstProductId(PendingPaymentStore.PendingPayment payment) {
        if (payment.items().isEmpty()) return "ORDER";
        String value = payment.items().getFirst().productId();
        value = value == null ? "" : value.replaceAll("[^A-Za-z0-9_-]", "");
        return value.isBlank() ? "ORDER" : value;
    }

    private static String limited(String value, int max) {
        if (value == null) return "";
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String url(String value) {
        return java.net.URLEncoder.encode(value == null ? "" : value, java.nio.charset.StandardCharsets.UTF_8);
    }

    private static void put(Map<String, String> fields, String name, String value) {
        fields.put(name, value == null ? "" : value);
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    public record Result(
            String paymentId,
            long amount,
            String paymentMethod,
            String result,
            String paymentInfoKey,
            String errorCode
    ) {}

    public enum PaymentState { PAID, PENDING, FAILED }
}
