package com.zik00.shop.domain.auth;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;

@Getter
@Entity
@Table(
        name = "oauth_access_tokens",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_oauth_access_tokens_user_provider",
                columnNames = {"user_id", "provider"}
        )
)
public class OAuthAccessToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "oauth_token_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(nullable = false, length = 30)
    private String provider;

    @Lob
    @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
    private String encryptedAccessToken;

    @Column(name = "issued_at")
    private Instant issuedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(length = 1000)
    private String scopes;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected OAuthAccessToken() {
    }

    public OAuthAccessToken(long userId, String provider) {
        this.userId = userId;
        this.provider = provider;
    }

    public void rotate(
            String encryptedAccessToken,
            Instant issuedAt,
            Instant expiresAt,
            String scopes
    ) {
        this.encryptedAccessToken = encryptedAccessToken;
        this.issuedAt = issuedAt;
        this.expiresAt = expiresAt;
        this.scopes = scopes;
        this.updatedAt = Instant.now();
    }
}
