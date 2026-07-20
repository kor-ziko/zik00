package com.zik00.shop.service;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class PostalCodeRequestRateLimiter {
    private static final int REQUESTS_PER_SECOND = 5;
    private static final int MAX_TRACKED_CLIENTS = 10_000;
    private static final long WINDOW_NANOS = Duration.ofSeconds(1).toNanos();
    private static final long CLIENT_EXPIRY_NANOS = Duration.ofMinutes(5).toNanos();
    private static final long CLEANUP_INTERVAL_NANOS = Duration.ofMinutes(1).toNanos();
    private static final String OVERFLOW_CLIENT = "__overflow__";

    private final ConcurrentHashMap<String, RequestWindow> requestWindows = new ConcurrentHashMap<>();
    private final AtomicLong lastCleanupNanos = new AtomicLong(System.nanoTime());

    public void check(HttpServletRequest request) {
        long now = System.nanoTime();
        cleanExpiredClients(now);

        String clientAddress = resolveClientAddress(request);
        if (!requestWindows.containsKey(clientAddress) && requestWindows.size() >= MAX_TRACKED_CLIENTS) {
            clientAddress = OVERFLOW_CLIENT;
        }

        AtomicBoolean allowed = new AtomicBoolean();
        requestWindows.compute(clientAddress, (key, current) -> {
            if (current == null || now - current.windowStartedNanos() >= WINDOW_NANOS) {
                allowed.set(true);
                return new RequestWindow(now, 1, now);
            }
            if (current.requestCount() < REQUESTS_PER_SECOND) {
                allowed.set(true);
                return new RequestWindow(current.windowStartedNanos(), current.requestCount() + 1, now);
            }
            return new RequestWindow(current.windowStartedNanos(), current.requestCount(), now);
        });

        if (!allowed.get()) {
            throw new ExternalApiRateLimitException("우편번호 조회 요청이 너무 많습니다. 잠시 후 다시 시도해주세요.");
        }
    }

    private void cleanExpiredClients(long now) {
        long previousCleanup = lastCleanupNanos.get();
        if (now - previousCleanup < CLEANUP_INTERVAL_NANOS
                || !lastCleanupNanos.compareAndSet(previousCleanup, now)) {
            return;
        }
        requestWindows.entrySet().removeIf(entry -> now - entry.getValue().lastSeenNanos() >= CLIENT_EXPIRY_NANOS);
    }

    private String resolveClientAddress(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (!isLoopback(remoteAddress)) {
            return remoteAddress;
        }

        String forwardedAddress = firstForwardedAddress(request.getHeader("X-Forwarded-For"));
        if (forwardedAddress != null) {
            return forwardedAddress;
        }
        String realAddress = validAddress(request.getHeader("X-Real-IP"));
        return realAddress == null ? remoteAddress : realAddress;
    }

    private String firstForwardedAddress(String value) {
        if (value == null) {
            return null;
        }
        int separator = value.indexOf(',');
        return validAddress(separator < 0 ? value : value.substring(0, separator));
    }

    private String validAddress(String value) {
        if (value == null) {
            return null;
        }
        String candidate = value.trim();
        if (candidate.isEmpty() || candidate.length() > 45) {
            return null;
        }
        for (int index = 0; index < candidate.length(); index++) {
            char character = candidate.charAt(index);
            if (!Character.isDigit(character)
                    && character != '.'
                    && character != ':'
                    && (character < 'a' || character > 'f')
                    && (character < 'A' || character > 'F')) {
                return null;
            }
        }
        return candidate;
    }

    private boolean isLoopback(String address) {
        return "127.0.0.1".equals(address) || "0:0:0:0:0:0:0:1".equals(address) || "::1".equals(address);
    }

    private record RequestWindow(long windowStartedNanos, int requestCount, long lastSeenNanos) {
    }
}
