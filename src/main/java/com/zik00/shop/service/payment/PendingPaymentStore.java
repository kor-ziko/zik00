package com.zik00.shop.service.payment;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class PendingPaymentStore {
    private static final String KEY_PREFIX = "shop:payment:pending:";
    private static final Duration TTL = Duration.ofDays(30);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public PendingPaymentStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public void save(PendingPayment payment) {
        try {
            redisTemplate.opsForValue().set(key(payment.paymentId()), objectMapper.writeValueAsString(payment), TTL);
        } catch (JacksonException exception) {
            throw new IllegalStateException("결제 준비 정보를 저장하지 못했습니다.", exception);
        }
    }

    public Optional<PendingPayment> find(String paymentId) {
        String value = redisTemplate.opsForValue().get(key(paymentId));
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, PendingPayment.class));
        } catch (JacksonException exception) {
            throw new IllegalStateException("결제 준비 정보를 읽지 못했습니다.", exception);
        }
    }

    public void delete(String paymentId) {
        redisTemplate.delete(key(paymentId));
    }

    private String key(String paymentId) {
        return KEY_PREFIX + paymentId;
    }

    public record PendingPayment(
            String paymentId,
            long memberId,
            long deliveryAddressId,
            String orderName,
            long totalAmount,
            String currency,
            List<PendingItem> items,
            long productAmount,
            long domesticShippingFee,
            long agencyFee,
            long estimatedShippingFee,
            long estimatedDuty,
            long estimatedConsumptionTax,
            long estimatedImportCharges,
            boolean customsFinalizationRequired
    ) {}

    public record PendingItem(
            long cartItemId,
            String productId,
            String productName,
            long unitPrice,
            int quantity,
            long subtotal,
            Map<String, String> selectedOptions
    ) {}
}
