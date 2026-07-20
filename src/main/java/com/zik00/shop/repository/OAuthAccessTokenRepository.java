package com.zik00.shop.repository;

import java.util.Optional;

import com.zik00.shop.domain.auth.OAuthAccessToken;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthAccessTokenRepository extends JpaRepository<OAuthAccessToken, Long> {
    Optional<OAuthAccessToken> findByUserIdAndProvider(long userId, String provider);
}
