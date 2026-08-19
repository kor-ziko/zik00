package com.zik00.shop.service.payment;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class SbPaymentProperties {
    private final boolean enabled;
    private final String merchantId;
    private final String serviceId;
    private final String hashKey;
    private final String requestUrl;
    private final String callbackBaseUrl;
    private final String clientBaseUrl;
    private final List<SbPaymentMethod> paymentMethods;

    public SbPaymentProperties(
            @Value("${shop.payment.sbps.enabled:false}") boolean enabled,
            @Value("${shop.payment.sbps.merchant-id:}") String merchantId,
            @Value("${shop.payment.sbps.service-id:}") String serviceId,
            @Value("${shop.payment.sbps.hash-key:}") String hashKey,
            @Value("${shop.payment.sbps.request-url:}") String requestUrl,
            @Value("${shop.payment.sbps.callback-base-url:}") String callbackBaseUrl,
            @Value("${shop.payment.sbps.client-base-url:http://localhost:5174}") String clientBaseUrl,
            @Value("${shop.payment.sbps.payment-methods:credit3d2,paypay,paypal}") String paymentMethods
    ) {
        this.enabled = enabled;
        this.merchantId = clean(merchantId);
        this.serviceId = clean(serviceId);
        this.hashKey = clean(hashKey);
        this.requestUrl = clean(requestUrl);
        this.callbackBaseUrl = withoutTrailingSlash(callbackBaseUrl);
        this.clientBaseUrl = withoutTrailingSlash(clientBaseUrl);
        this.paymentMethods = parsePaymentMethods(paymentMethods);
    }

    public boolean configured() {
        return enabled
                && merchantId.matches("\\d{5}")
                && serviceId.matches("\\d{3}")
                && !hashKey.isBlank()
                && isHttps(requestUrl)
                && isHttps(callbackBaseUrl)
                && isHttp(clientBaseUrl)
                && !paymentMethods.isEmpty();
    }

    public String merchantId() { return merchantId; }
    public String serviceId() { return serviceId; }
    public String hashKey() { return hashKey; }
    public String requestUrl() { return requestUrl; }
    public String callbackBaseUrl() { return callbackBaseUrl; }
    public String clientBaseUrl() { return clientBaseUrl; }
    public List<SbPaymentMethod> paymentMethods() { return paymentMethods; }

    private static List<SbPaymentMethod> parsePaymentMethods(String value) {
        LinkedHashSet<SbPaymentMethod> methods = new LinkedHashSet<>();
        Arrays.stream(clean(value).split(","))
                .map(String::trim)
                .map(SbPaymentMethod::fromCode)
                .flatMap(Optional::stream)
                .forEach(methods::add);
        return List.copyOf(methods);
    }

    private static boolean isHttps(String value) {
        return hasScheme(value, "https");
    }

    private static boolean isHttp(String value) {
        return hasScheme(value, "http") || hasScheme(value, "https");
    }

    private static boolean hasScheme(String value, String scheme) {
        try {
            return scheme.equalsIgnoreCase(URI.create(value).getScheme());
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private static String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private static String withoutTrailingSlash(String value) {
        String cleaned = clean(value);
        while (cleaned.endsWith("/")) cleaned = cleaned.substring(0, cleaned.length() - 1);
        return cleaned;
    }
}
