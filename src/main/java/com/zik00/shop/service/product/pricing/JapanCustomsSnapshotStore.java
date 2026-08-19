package com.zik00.shop.service.product.pricing;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class JapanCustomsSnapshotStore {
    private static final String KEY = "shop:customs:japan:latest";
    private static final Duration RETENTION = Duration.ofDays(60);

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public JapanCustomsSnapshotStore(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public Optional<JapanCustomsSnapshot> find() {
        String value = redisTemplate.opsForValue().get(KEY);
        if (value == null || value.isBlank()) return Optional.empty();
        try {
            return Optional.of(objectMapper.readValue(value, JapanCustomsSnapshot.class));
        } catch (JacksonException exception) {
            return Optional.empty();
        }
    }

    public void save(JapanCustomsSnapshot snapshot) {
        try {
            redisTemplate.opsForValue().set(KEY, objectMapper.writeValueAsString(snapshot), RETENTION);
        } catch (JacksonException exception) {
            throw new IllegalStateException("일본 세관 기준을 저장하지 못했습니다.", exception);
        }
    }
}
