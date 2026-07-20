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
    private static final String VALUE_SEPARATOR = "\n";

    private final UserRepository userRepository;
    private final JwtSessionService jwtSessionService;
    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public OAuthLoginCompletionService(
            UserRepository userRepository,
            JwtSessionService jwtSessionService,
            StringRedisTemplate redisTemplate
    ) {
        this.userRepository = userRepository;
        this.jwtSessionService = jwtSessionService;
        this.redisTemplate = redisTemplate;
    }

    public String prepare(User user, String destination) {
        String safeDestination = normalizeDestination(destination);
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String code = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        redisTemplate.opsForValue().set(
                key(code),
                user.getAccessId() + VALUE_SEPARATOR + safeDestination,
                CODE_TTL
        );
        return code;
    }

    public CompletionResult complete(String code, HttpServletResponse response) {
        if (code == null || code.isBlank() || code.length() > 128) {
            throw invalidCompletionCode();
        }

        String storedValue = redisTemplate.opsForValue().getAndDelete(key(code));
        PendingLogin pendingLogin = decode(storedValue);
        if (pendingLogin == null) {
            throw invalidCompletionCode();
        }

        User user = userRepository.findByAccessId(pendingLogin.accessId())
                .orElseThrow(() -> new InvalidJwtException("OAuth 로그인 회원을 찾을 수 없습니다."));
        JwtSessionService.AccessTokenResult token = jwtSessionService.issue(user, response);
        return new CompletionResult(token.accessToken(), token.expiresAt(), pendingLogin.destination());
    }

    private PendingLogin decode(String value) {
        if (value == null) {
            return null;
        }
        int separatorIndex = value.indexOf(VALUE_SEPARATOR);
        if (separatorIndex <= 0 || separatorIndex == value.length() - 1) {
            return null;
        }
        String accessId = value.substring(0, separatorIndex);
        String destination = normalizeDestination(value.substring(separatorIndex + 1));
        return new PendingLogin(accessId, destination);
    }

    private String normalizeDestination(String destination) {
        return "/login/additional-info".equals(destination) ? destination : "/";
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

    private record PendingLogin(String accessId, String destination) {
    }
}
