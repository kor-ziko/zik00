package com.zik00.shop.service.auth;

import java.time.Instant;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import com.zik00.shop.domain.User;
import com.zik00.shop.domain.auth.OAuthAccessToken;
import com.zik00.shop.repository.OAuthAccessTokenRepository;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OAuthAccessTokenService {
    private static final String GOOGLE = "google";

    private final OAuthAccessTokenRepository tokenRepository;
    private final TokenEncryptionService encryptionService;

    public OAuthAccessTokenService(
            OAuthAccessTokenRepository tokenRepository,
            TokenEncryptionService encryptionService
    ) {
        this.tokenRepository = tokenRepository;
        this.encryptionService = encryptionService;
    }

    @Transactional
    public void saveGoogleAccessToken(User user, OAuth2AccessToken accessToken) {
        OAuthAccessToken storedToken = tokenRepository
                .findByUserIdAndProvider(user.getMemberId(), GOOGLE)
                .orElseGet(() -> new OAuthAccessToken(user.getMemberId(), GOOGLE));
        storedToken.rotate(
                encryptionService.encrypt(accessToken.getTokenValue()),
                accessToken.getIssuedAt(),
                accessToken.getExpiresAt(),
                accessToken.getScopes().stream()
                        .sorted(Comparator.naturalOrder())
                        .collect(Collectors.joining(" "))
        );
        tokenRepository.save(storedToken);
    }

    @Transactional(readOnly = true)
    public Optional<String> findValidGoogleAccessToken(long userId) {
        return tokenRepository.findByUserIdAndProvider(userId, GOOGLE)
                .filter(token -> token.getExpiresAt() == null || token.getExpiresAt().isAfter(Instant.now()))
                .map(OAuthAccessToken::getEncryptedAccessToken)
                .map(encryptionService::decrypt);
    }
}
