package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class OAuthLoginCompletionService {
    private static final Duration CODE_TTL = Duration.ofMinutes(2);
    private static final String KEY_PREFIX = "shop:auth:oauth-completion:";
    private static final String EXISTING = "existing";
    private static final String REGISTRATION = "registration";
    private static final String SEPARATOR = "\n";
    private static final String SESSION_BINDING_ATTRIBUTE =
            OAuthLoginCompletionService.class.getName() + ".completion-code";

    private final UserRepository userRepository;
    private final JwtSessionService jwtSessionService;
    private final JwtCookieService jwtCookieService;
    private final PendingOAuthRegistrationService pendingRegistrationService;
    private final StringRedisTemplate redisTemplate;
    private final OpaqueTokenCodec tokenCodec;

    public OAuthLoginCompletionService(
            UserRepository userRepository,
            JwtSessionService jwtSessionService,
            JwtCookieService jwtCookieService,
            PendingOAuthRegistrationService pendingRegistrationService,
            StringRedisTemplate redisTemplate,
            OpaqueTokenCodec tokenCodec
    ) {
        this.userRepository = userRepository;
        this.jwtSessionService = jwtSessionService;
        this.jwtCookieService = jwtCookieService;
        this.pendingRegistrationService = pendingRegistrationService;
        this.redisTemplate = redisTemplate;
        this.tokenCodec = tokenCodec;
    }

    public String prepareExisting(User user) {
        return prepare(EXISTING + SEPARATOR + user.getAccessId());
    }

    public String prepareRegistration(OAuthProfile profile) {
        return prepare(REGISTRATION + SEPARATOR
                + encodePart(profile.provider()) + SEPARATOR
                + encodePart(profile.subject()) + SEPARATOR
                + encodePart(profile.email()) + SEPARATOR
                + encodePart(profile.displayName()));
    }

    public void bindToNewSession(String code, HttpServletRequest request) {
        HttpSession currentSession = request.getSession(false);
        if (currentSession != null) {
            currentSession.invalidate();
        }
        request.getSession(true).setAttribute(SESSION_BINDING_ATTRIBUTE, fingerprint(code));
    }

    public CompletionResult complete(
            String code,
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        if (code == null || code.isBlank() || code.length() > 128) {
            throw invalidCompletionCode();
        }

        requireSessionBinding(code, request);

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
            OAuthProfile account = decodeAccount(parts);
            jwtCookieService.clearRefreshToken(response);
            pendingRegistrationService.issue(account, response);
            return new CompletionResult(null, null, "/login/terms");
        }

        throw invalidCompletionCode();
    }

    private String prepare(String value) {
        String code = tokenCodec.newToken();
        redisTemplate.opsForValue().set(key(code), value, CODE_TTL);
        return code;
    }

    private OAuthProfile decodeAccount(String[] parts) {
        try {
            boolean legacyGoogleValue = parts.length == 4;
            String provider = legacyGoogleValue ? "google" : decodePart(parts[1]);
            int subjectIndex = legacyGoogleValue ? 1 : 2;
            String subject = decodePart(parts[subjectIndex]);
            if (subject.isBlank()) {
                throw invalidCompletionCode();
            }
            return new OAuthProfile(
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
        return tokenCodec.encode(value);
    }

    private String decodePart(String value) {
        return tokenCodec.decode(value);
    }

    private String key(String code) {
        return tokenCodec.redisKey(KEY_PREFIX, code);
    }

    private void requireSessionBinding(String code, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Object expectedValue = session == null
                ? null
                : session.getAttribute(SESSION_BINDING_ATTRIBUTE);
        if (!(expectedValue instanceof String expected)
                || !MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.US_ASCII),
                fingerprint(code).getBytes(StandardCharsets.US_ASCII))) {
            throw invalidCompletionCode();
        }
        session.removeAttribute(SESSION_BINDING_ATTRIBUTE);
    }

    private String fingerprint(String code) {
        return tokenCodec.redisKey("", code);
    }

    private InvalidJwtException invalidCompletionCode() {
        return new InvalidJwtException("OAuth 로그인 완료 코드가 만료되었거나 유효하지 않습니다.");
    }

    public record CompletionResult(String accessToken, Instant expiresAt, String destination) {
    }
}
