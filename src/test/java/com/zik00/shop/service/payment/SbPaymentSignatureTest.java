package com.zik00.shop.service.payment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SbPaymentSignatureTest {
    @Test
    void createsStableRequestHashInFieldOrder() {
        Map<String, String> fields = new LinkedHashMap<>();
        fields.put("merchant_id", "12345");
        fields.put("service_id", "001");

        String signature = SbPaymentSignature.request(
                fields, List.of("merchant_id", "service_id"), "secret"
        );

        assertThat(signature).isEqualTo("91ba537369501765d0237621e3ba9155176942dd");
    }

    @Test
    void responseComparisonAcceptsUppercaseSbPaymentSignature() {
        Map<String, String> fields = Map.of("res_result", "OK", "order_id", "payment1");
        List<String> order = List.of("order_id", "res_result");
        String signature = SbPaymentSignature.request(fields, order, "secret").toUpperCase();

        assertThat(SbPaymentSignature.responseMatches(fields, order, "secret", signature)).isTrue();
        assertThat(SbPaymentSignature.responseMatches(fields, order, "secret", signature + "0")).isFalse();
    }
}
