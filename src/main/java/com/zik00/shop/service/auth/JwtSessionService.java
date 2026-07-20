package com.zik00.shop.service.auth;

import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
        JwtService.JwtPair pair = jwtService.issue(user.getAccessId());
        refreshTokenStore.save(jwtService.hash(pair.refreshToken()), user.getAccessId(), pair.refreshExpiresAt());
        cookieService.writeRefreshToken(response, pair);
        return new AccessTokenResult(pair.accessToken(), pair.accessExpiresAt());
    }

    public AccessTokenResult refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieService.readRefreshToken(request)
                .orElseThrow(() -> new InvalidJwtException("Refresh Token이 없습니다."));
        JwtService.JwtClaims claims = jwtService.validate(refreshTokenValue, JwtService.REFRESH);
        String storedAccessId = refreshTokenStore.consume(jwtService.hash(refreshTokenValue))
                .orElseThrow(() -> new InvalidJwtException("Refresh Token이 만료되었거나 폐기되었습니다."));
        User user = userRepository.findByAccessId(claims.accessId())
                .filter(found -> found.getAccessId().equals(storedAccessId))
                .orElseThrow(() -> new InvalidJwtException("Refresh Token 회원을 찾을 수 없습니다."));

        return issue(user, response);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        try {
            cookieService.readRefreshToken(request)
                    .ifPresent(token -> refreshTokenStore.revoke(jwtService.hash(token)));
        } finally {
            cookieService.clearRefreshToken(response);
        }
    }

    public record AccessTokenResult(String accessToken, java.time.Instant expiresAt) {
    }
}
