package com.zik00.shop.service.auth;

import java.time.Instant;

import com.zik00.shop.domain.User;
import com.zik00.shop.domain.auth.RefreshToken;
import com.zik00.shop.repository.RefreshTokenRepository;
import com.zik00.shop.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class JwtSessionService {
    private final JwtService jwtService;
    private final JwtCookieService cookieService;
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    public JwtSessionService(
            JwtService jwtService,
            JwtCookieService cookieService,
            RefreshTokenRepository refreshTokenRepository,
            UserRepository userRepository
    ) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void issue(User user, HttpServletResponse response) {
        JwtService.JwtPair pair = jwtService.issue(user.getAccessId());
        refreshTokenRepository.save(new RefreshToken(
                user.getMemberId(),
                jwtService.hash(pair.refreshToken()),
                pair.refreshExpiresAt()
        ));
        cookieService.writeTokens(response, pair);
    }

    @Transactional
    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshTokenValue = cookieService.readRefreshToken(request)
                .orElseThrow(() -> new InvalidJwtException("Refresh Token이 없습니다."));
        JwtService.JwtClaims claims = jwtService.validate(refreshTokenValue, JwtService.REFRESH);
        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(jwtService.hash(refreshTokenValue))
                .filter(token -> token.isUsable(Instant.now()))
                .orElseThrow(() -> new InvalidJwtException("Refresh Token이 만료되었거나 폐기되었습니다."));
        User user = userRepository.findByAccessId(claims.accessId())
                .filter(found -> found.getMemberId() == storedToken.getUserId())
                .orElseThrow(() -> new InvalidJwtException("Refresh Token 회원을 찾을 수 없습니다."));

        storedToken.revoke();
        issue(user, response);
    }

    @Transactional
    public void logout(HttpServletRequest request, HttpServletResponse response) {
        cookieService.readRefreshToken(request)
                .flatMap(token -> refreshTokenRepository.findByTokenHash(jwtService.hash(token)))
                .ifPresent(RefreshToken::revoke);
        cookieService.clearTokens(response);
    }
}
