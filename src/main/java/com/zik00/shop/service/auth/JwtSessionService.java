package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
public class JwtSessionService {
    private final JwtService jwtService;
    private final JwtCookieService cookieService;
    private final RedisRefreshTokenStore refreshTokenStore;
    private final UserRepository userRepository;

    public JwtSessionService(
            JwtService jwtService,
            JwtCookieService cookieService,
            RedisRefreshTokenStore refreshTokenStore,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.refreshTokenStore = refreshTokenStore;
        this.userRepository = userRepository;
    }

    public AccessTokenResult issue(User user, HttpServletResponse response) {
        refreshTokenStore.revokeAllForUser(user.getAccessId());
        JwtService.JwtPair pair = jwtService.issue(user.getAccessId());
        persist(user, pair, response);
        return new AccessTokenResult(pair.accessToken(), pair.accessExpiresAt());
    }

    private AccessTokenResult rotate(User user, String familyId, HttpServletResponse response) {
        JwtService.JwtPair pair = jwtService.rotate(user.getAccessId(), familyId);
        persist(user, pair, response);
        return new AccessTokenResult(pair.accessToken(), pair.accessExpiresAt());
    }

    private void persist(User user, JwtService.JwtPair pair, HttpServletResponse response) {
        refreshTokenStore.save(pair, jwtService.hash(pair.refreshToken()), user.getAccessId());
        cookieService.writeRefreshToken(response, pair);
    }

    public AccessTokenResult refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieService.readRefreshToken(request)
                .orElseThrow(() -> new InvalidJwtException("Refresh Token이 없습니다."));
        JwtService.JwtClaims claims = jwtService.validate(refreshTokenValue, JwtService.REFRESH);
        RedisRefreshTokenStore.ConsumeResult consumed = refreshTokenStore.consume(
                jwtService.hash(refreshTokenValue),
                claims.familyId(),
                claims.expiresAt()
        );
        if (consumed.status() == RedisRefreshTokenStore.ConsumeStatus.REUSED) {
            refreshTokenStore.revokeFamily(consumed.familyId());
            throw new InvalidJwtException("Refresh Token 재사용이 감지되어 로그인 세션 전체를 폐기했습니다.");
        }
        if (consumed.status() == RedisRefreshTokenStore.ConsumeStatus.MISSING) {
            throw new InvalidJwtException("Refresh Token이 만료되었거나 폐기되었습니다.");
        }
        if (!claims.familyId().equals(consumed.familyId())
                || !claims.accessId().equals(consumed.accessId())) {
            refreshTokenStore.revokeFamily(claims.familyId());
            refreshTokenStore.revokeFamily(consumed.familyId());
            throw new InvalidJwtException("Refresh Token 로그인 세션 정보가 올바르지 않습니다.");
        }
        User user = userRepository.findByAccessId(claims.accessId()).orElse(null);
        if (user == null) {
            refreshTokenStore.revokeFamily(claims.familyId());
            throw new InvalidJwtException("Refresh Token 회원을 찾을 수 없습니다.");
        }

        return rotate(user, claims.familyId(), response);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            cookieService.readRefreshToken(request)
                    .ifPresent(token -> revokeTokenFamily(token, JwtService.REFRESH));
            readBearerToken(request)
                    .ifPresent(token -> revokeTokenFamily(token, JwtService.ACCESS));
        } finally {
            cookieService.clearRefreshToken(response);
            if (request.getSession(false) != null) {
                request.getSession(false).invalidate();
            }
            SecurityContextHolder.clearContext();
        }
    }

    private void revokeTokenFamily(String token, String expectedType) {
        try {
            JwtService.JwtClaims claims = jwtService.validate(token, expectedType);
            refreshTokenStore.revokeAllForUser(claims.accessId());
            refreshTokenStore.revokeFamily(claims.familyId());
        } catch (InvalidJwtException ignored) {
            // 이미 만료되거나 변조된 토큰은 서버 세션을 식별하는 데 사용하지 않는다.
        }
    }

    private java.util.Optional<String> readBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return java.util.Optional.empty();
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }

    public record AccessTokenResult(String accessToken, java.time.Instant expiresAt) {
    }
}
