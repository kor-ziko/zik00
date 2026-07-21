package com.zik00.shop.service.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class PendingOAuthRegistrationService {
    private static final String COOKIE_NAME = "zik_pending_registration";
    private static final String COOKIE_PATH = "/api/auth";
    private static final String LEGACY_COOKIE_PATH = "/api/auth/detail";
    private static final String KEY_PREFIX = "shop:auth:pending-registration:";
    private static final Duration TTL = Duration.ofMinutes(30);
    private static final String SEPARATOR = ".";
    private static final java.util.Set<String> SUPPORTED_PROVIDERS = java.util.Set.of("google", "kakao", "line");

    private final StringRedisTemplate redisTemplate;
    private final boolean secure;
    private final SecureRandom secureRandom = new SecureRandom();

    public PendingOAuthRegistrationService(
            StringRedisTemplate redisTemplate,
            @Value("${shop.jwt.cookie-secure:false}") boolean secure
    ) {
        this.redisTemplate = redisTemplate;
        this.secure = secure;
    }

    public void issue(PendingOAuthAccount account, HttpServletResponse response) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String token = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        redisTemplate.opsForValue().set(key(token), encode(new PendingRegistration(account, false, false)), TTL);
        clearLegacyCookie(response);
        writeCookie(response, token, TTL, COOKIE_PATH);
    }

    public PendingRegistration require(HttpServletRequest request) {
        return decodeRequired(readStoredValue(request));
    }

    public void acceptTerms(
            HttpServletRequest request,
            HttpServletResponse response,
            boolean alarmConsent
    ) {
        String token = readToken(request).orElseThrow(this::invalidRegistration);
        String redisKey = key(token);
        PendingRegistration pending = decodeRequired(redisTemplate.opsForValue().get(redisKey));
        Long remainingSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
        if (remainingSeconds == null || remainingSeconds <= 0) {
            throw invalidRegistration();
        }
        redisTemplate.opsForValue().set(
                redisKey,
                encode(new PendingRegistration(pending.account(), true, alarmConsent)),
                Duration.ofSeconds(remainingSeconds)
        );
        clearLegacyCookie(response);
    }

    public AcceptedOAuthRegistration requireTermsAccepted(HttpServletRequest request) {
        PendingRegistration pending = require(request);
        if (!pending.termsAccepted()) {
            throw new RegistrationTermsRequiredException();
        }
        return new AcceptedOAuthRegistration(pending.account(), pending.alarmConsent());
    }

    public AcceptedOAuthRegistration consumeTermsAccepted(HttpServletRequest request, HttpServletResponse response) {
        String token = readToken(request).orElseThrow(this::invalidRegistration);
        PendingRegistration pending = decodeRequired(redisTemplate.opsForValue().getAndDelete(key(token)));
        clear(response);
        if (!pending.termsAccepted()) {
            throw new RegistrationTermsRequiredException();
        }
        return new AcceptedOAuthRegistration(pending.account(), pending.alarmConsent());
    }

    public void clear(HttpServletResponse response) {
        writeCookie(response, "", Duration.ZERO, COOKIE_PATH);
        clearLegacyCookie(response);
    }

    private Optional<String> readToken(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> COOKIE_NAME.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private String readStoredValue(HttpServletRequest request) {
        return readToken(request)
                .map(token -> redisTemplate.opsForValue().get(key(token)))
                .orElse(null);
    }

    private String encode(PendingRegistration pending) {
        PendingOAuthAccount account = pending.account();
        return encodePart(account.provider()) + SEPARATOR
                + encodePart(account.subject()) + SEPARATOR
                + encodePart(account.email()) + SEPARATOR
                + encodePart(account.displayName()) + SEPARATOR
                + (pending.termsAccepted() ? "1" : "0") + SEPARATOR
                + (pending.alarmConsent() ? "1" : "0");
    }

    private PendingRegistration decodeRequired(String value) {
        if (value == null) {
            throw invalidRegistration();
        }
        String[] parts = value.split("\\.", -1);
        boolean legacyGoogleValue = parts.length == 4 || parts.length == 5;
        int termsIndex = legacyGoogleValue ? 3 : 4;
        int alarmIndex = legacyGoogleValue ? 4 : 5;
        if ((!legacyGoogleValue && parts.length != 6)
                || !("0".equals(parts[termsIndex]) || "1".equals(parts[termsIndex]))
                || (parts.length > alarmIndex
                && !("0".equals(parts[alarmIndex]) || "1".equals(parts[alarmIndex])))) {
            throw invalidRegistration();
        }
        try {
            PendingOAuthAccount account = legacyGoogleValue
                    ? new PendingOAuthAccount(
                            "google",
                            decodePart(parts[0]),
                            decodePart(parts[1]),
                            decodePart(parts[2])
                    )
                    : new PendingOAuthAccount(
                            decodePart(parts[0]),
                            decodePart(parts[1]),
                            decodePart(parts[2]),
                            decodePart(parts[3])
                    );
            if (!SUPPORTED_PROVIDERS.contains(account.provider()) || account.subject().isBlank()) {
                throw invalidRegistration();
            }
            return new PendingRegistration(
                    account,
                    "1".equals(parts[termsIndex]),
                    parts.length > alarmIndex && "1".equals(parts[alarmIndex])
            );
        } catch (IllegalArgumentException exception) {
            throw invalidRegistration();
        }
    }

    private String encodePart(String value) {
        String normalized = value == null ? "" : value;
        return Base64.getUrlEncoder().withoutPadding()
                .encodeToString(normalized.getBytes(StandardCharsets.UTF_8));
    }

    private String decodePart(String value) {
        return new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
    }

    private void clearLegacyCookie(HttpServletResponse response) {
        writeCookie(response, "", Duration.ZERO, LEGACY_COOKIE_PATH);
    }

    private void writeCookie(HttpServletResponse response, String value, Duration maxAge, String path) {
        ResponseCookie cookie = ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private String key(String token) {
        return KEY_PREFIX + toHex(sha256(token));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("가입 토큰을 해시할 수 없습니다.", exception);
        }
    }

    private String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

    private InvalidJwtException invalidRegistration() {
        return new InvalidJwtException("가입 정보가 만료되었거나 이미 사용되었습니다. OAuth 로그인을 다시 진행해주세요.");
    }

    public record PendingOAuthAccount(String provider, String subject, String email, String displayName) {
    }

    public record PendingRegistration(
            PendingOAuthAccount account,
            boolean termsAccepted,
            boolean alarmConsent
    ) {
    }

    public record AcceptedOAuthRegistration(PendingOAuthAccount account, boolean alarmConsent) {
    }
}
