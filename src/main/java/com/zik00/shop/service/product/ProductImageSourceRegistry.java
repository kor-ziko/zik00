package com.zik00.shop.service.product;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ProductImageSourceRegistry {
    private static final Duration REGISTRATION_TTL = Duration.ofDays(7);
    private static final String REDIS_PREFIX = "shop:product-images:allowed:";
    private final ConcurrentHashMap<String, Instant> registeredUrls = new ConcurrentHashMap<>();
    private final StringRedisTemplate redisTemplate;

    public ProductImageSourceRegistry() {
        this.redisTemplate = null;
    }

    @Autowired
    public ProductImageSourceRegistry(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void register(String imageUrl) {
        URI uri = parseHttps(imageUrl);
        if (uri == null) return;
        registeredUrls.put(uri.toString(), Instant.now().plus(REGISTRATION_TTL));
        if (redisTemplate != null) {
            try {
                redisTemplate.opsForValue().set(redisKey(uri), "1", REGISTRATION_TTL);
            } catch (RuntimeException ignored) {
                // The in-memory registration still supports the current server process.
            }
        }
    }

    public boolean isRegistered(String imageUrl) {
        URI uri = parseHttps(imageUrl);
        if (uri == null) return false;
        Instant expiresAt = registeredUrls.get(uri.toString());
        if (expiresAt != null && expiresAt.isBefore(Instant.now())) {
            registeredUrls.remove(uri.toString(), expiresAt);
            expiresAt = null;
        }
        if (expiresAt != null) return true;
        if (redisTemplate == null) return false;
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(redisKey(uri)));
        } catch (RuntimeException ignored) {
            return false;
        }
    }

    private URI parseHttps(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value);
            boolean valid = "https".equalsIgnoreCase(uri.getScheme())
                    && uri.getHost() != null
                    && uri.getUserInfo() == null
                    && (uri.getPort() == -1 || uri.getPort() == 443);
            return valid ? uri : null;
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private String redisKey(URI uri) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(uri.toString().getBytes(StandardCharsets.UTF_8));
            return REDIS_PREFIX + java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
