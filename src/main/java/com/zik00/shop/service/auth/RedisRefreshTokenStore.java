package com.zik00.shop.service.auth;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedisRefreshTokenStore {
    private static final String KEY_PREFIX = "shop:auth:refresh:";

    private final StringRedisTemplate redisTemplate;

    public RedisRefreshTokenStore(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void save(String tokenHash, String accessId, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            throw new InvalidJwtException("Refresh Token 만료 시간이 올바르지 않습니다.");
        }
        redisTemplate.opsForValue().set(key(tokenHash), accessId, ttl);
    }

    public Optional<String> consume(String tokenHash) {
        return Optional.ofNullable(redisTemplate.opsForValue().getAndDelete(key(tokenHash)));
    }

    public void revoke(String tokenHash) {
        redisTemplate.delete(key(tokenHash));
    }

    private String key(String tokenHash) {
        return KEY_PREFIX + tokenHash;
    }
}
