package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OAuthLoginCompletionService {
    private static final Duration CODE_TTL = Duration.ofMinutes(2);
    private static final String KEY_PREFIX = "shop:auth:oauth-completion:";
    private static final String EXISTING = "existing";
    private static final String REGISTRATION = "registration";
    private static final String SEPARATOR = "\n";

    private final UserRepository userRepository;
    private final JwtSessionService jwtSessionService;
    private final JwtCookieService jwtCookieService;
    private final PendingOAuthRegistrationService pendingRegistrationService;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthLoginCompletionService(
            UserRepository userRepository,
            JwtSessionService jwtSessionService,
            JwtCookieService jwtCookieService,
            PendingOAuthRegistrationService pendingRegistrationService,
            StringRedisTemplate redisTemplate
    ) {
        this.userRepository = userRepository;
        this.jwtSessionService = jwtSessionService;
        this.jwtCookieService = jwtCookieService;
        this.pendingRegistrationService = pendingRegistrationService;
        this.redisTemplate = redisTemplate;
    }

    public String prepareExisting(User user) {
        return prepare(EXISTING + SEPARATOR + user.getAccessId());
    }

    public String prepareRegistration(String provider, String subject, String email, String displayName) {
        return prepare(REGISTRATION + SEPARATOR
                + encodePart(provider) + SEPARATOR
                + encodePart(subject) + SEPARATOR
                + encodePart(email) + SEPARATOR
                + encodePart(displayName));
    }

    public CompletionResult complete(String code, HttpServletResponse response) {
        if (code == null || code.isBlank() || code.length() > 128) {
            throw invalidCompletionCode();
        }

        String storedValue = redisTemplate.opsForValue().getAndDelete(key(code));
        if (storedValue == null) {
            throw invalidCompletionCode();
        }

        String[] parts = storedValue.split(SEPARATOR, -1);
        if (parts.length == 2 && EXISTING.equals(parts[0])) {
            User user = userRepository.findByAccessId(parts[1])
                    .orElseThrow(() -> new InvalidJwtException("OAuth 로그인 회원을 찾을 수 없습니다."));
            pendingRegistrationService.clear(response);
            JwtSessionService.AccessTokenResult token = jwtSessionService.issue(user, response);
            return new CompletionResult(token.accessToken(), token.expiresAt(), "/");
        }

        if ((parts.length == 4 || parts.length == 5) && REGISTRATION.equals(parts[0])) {
            PendingOAuthRegistrationService.PendingOAuthAccount account = decodeAccount(parts);
            jwtCookieService.clearRefreshToken(response);
            pendingRegistrationService.issue(account, response);
            return new CompletionResult(null, null, "/login/terms");
        }

        throw invalidCompletionCode();
    }

    private String prepare(String value) {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        redisTemplate.opsForValue().set(key(code), value, CODE_TTL);
        return code;
    }

    private PendingOAuthRegistrationService.PendingOAuthAccount decodeAccount(String[] parts) {
        try {
            boolean legacyGoogleValue = parts.length == 4;
            String provider = legacyGoogleValue ? "google" : decodePart(parts[1]);
            int subjectIndex = legacyGoogleValue ? 1 : 2;
            String subject = decodePart(parts[subjectIndex]);
            if (subject.isBlank()) {
                throw invalidCompletionCode();
            }
            return new PendingOAuthRegistrationService.PendingOAuthAccount(
                    provider,
                    subject,
                    decodePart(parts[subjectIndex + 1]),
                    decodePart(parts[subjectIndex + 2])
            );
        } catch (IllegalArgumentException exception) {
            throw invalidCompletionCode();
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

    private String key(String code) {
        return KEY_PREFIX + toHex(sha256(code));
    }

    private byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("OAuth 로그인 코드를 해시할 수 없습니다.", exception);
        }
    }

    private String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

    private InvalidJwtException invalidCompletionCode() {
        return new InvalidJwtException("OAuth 로그인 완료 코드가 만료되었거나 유효하지 않습니다.");
    }

    public record CompletionResult(String accessToken, Instant expiresAt, String destination) {
    }
}
