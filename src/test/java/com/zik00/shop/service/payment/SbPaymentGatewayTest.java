package com.zik00.shop.service.payment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class SbPaymentGatewayTest {
    @Test
    void sendsSelectedConfiguredPaymentMethodToSbps() {
        SbPaymentGateway gateway = gateway("credit3d2,paypay,paypal");

        var response = gateway.createRequest(payment(), "paypay");

        assertThat(response.fields().get("pay_method")).isEqualTo("paypay");
        assertThat(response.fields().get("sps_hashcode")).isNotBlank();
    }

    @Test
    void rejectsPaymentMethodThatIsNotConfigured() {
        SbPaymentGateway gateway = gateway("credit3d2");

        assertThatThrownBy(() -> gateway.createRequest(payment(), "paypal"))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("사용할 수 없는 결제수단");
    }

    @Test
    void exposesConfiguredMethodsInConfiguredOrder() {
        SbPaymentGateway gateway = gateway("paypal,credit3d2,paypal,unknown");

        assertThat(gateway.paymentMethods()).extracting("code")
                .containsExactly("paypal", "credit3d2");
    }

    private SbPaymentGateway gateway(String methods) {
        return new SbPaymentGateway(new SbPaymentProperties(
                true, "12345", "001", "secret",
                "https://example.com/payment", "https://api.example.com",
                "http://localhost:5174", methods
        ));
    }

    private PendingPaymentStore.PendingPayment payment() {
        return new PendingPaymentStore.PendingPayment(
                "payment123", 1L, 2L, "주문", 12000L, "JPY", List.of(),
                9000L, 0L, 0L, 3000L, 0L, 0L, 0L, false
        );
    }
}
