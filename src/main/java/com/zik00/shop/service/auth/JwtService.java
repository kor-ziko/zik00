package com.zik00.shop.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    private static final String HEADER = base64Url("{\"alg\":\"HS256\",\"typ\":\"JWT\"}");

    private final ObjectMapper objectMapper;
    private final SecretKeySpec signingKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${shop.jwt.secret}") String secret,
            @Value("${shop.jwt.access-token-ttl:PT15M}") Duration accessTtl,
            @Value("${shop.jwt.refresh-token-ttl:P14D}") Duration refreshTtl,
            @Value("${shop.jwt.issuer:zik00-shop}") String issuer
    ) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 characters.");
        }
        this.objectMapper = objectMapper;
        this.signingKey = new SecretKeySpec(sha256("zik00-jwt-v1\0" + secret), "HmacSHA256");
        this.accessTtl = accessTtl;
        this.refreshTtl = refreshTtl;
        this.issuer = issuer;
    }

    public JwtPair issue(String accessId) {
        Instant now = Instant.now();
        return new JwtPair(
                createToken(accessId, ACCESS, now, now.plus(accessTtl)),
                createToken(accessId, REFRESH, now, now.plus(refreshTtl)),
                now.plus(accessTtl),
                now.plus(refreshTtl)
        );
    }

    public JwtClaims validate(String token, String expectedType) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) {
                throw new InvalidJwtException("JWT 형식이 올바르지 않습니다.");
            }
            byte[] expectedSignature = sign(parts[0] + "." + parts[1]);
            byte[] actualSignature = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expectedSignature, actualSignature)) {
                throw new InvalidJwtException("JWT 서명이 올바르지 않습니다.");
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            String accessId = payload.path("sub").asString();
            String type = payload.path("type").asString();
            String tokenIssuer = payload.path("iss").asString();
            String tokenId = payload.path("jti").asString();
            Instant expiresAt = Instant.ofEpochSecond(payload.path("exp").asLong());
            if (accessId.isBlank() || tokenId.isBlank() || !issuer.equals(tokenIssuer)
                    || !expectedType.equals(type) || !Instant.now().isBefore(expiresAt)) {
                throw new InvalidJwtException("JWT가 만료되었거나 사용할 수 없습니다.");
            }
            return new JwtClaims(accessId, tokenId, type, expiresAt);
        } catch (JacksonException | IllegalArgumentException exception) {
            throw new InvalidJwtException("JWT를 해석할 수 없습니다.", exception);
        }
    }

    public String hash(String token) {
        return toHex(sha256(token));
    }

    private String createToken(String accessId, String type, Instant issuedAt, Instant expiresAt) {
        try {
            Map<String, Object> claims = new LinkedHashMap<>();
            claims.put("iss", issuer);
            claims.put("sub", accessId);
            claims.put("type", type);
            claims.put("jti", UUID.randomUUID().toString());
            claims.put("iat", issuedAt.getEpochSecond());
            claims.put("exp", expiresAt.getEpochSecond());
            String payload = base64Url(objectMapper.writeValueAsString(claims));
            String unsignedToken = HEADER + "." + payload;
            return unsignedToken + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(sign(unsignedToken));
        } catch (JacksonException exception) {
            throw new IllegalStateException("JWT 생성에 실패했습니다.", exception);
        }
    }

    private byte[] sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(signingKey);
            return mac.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("JWT 서명에 실패했습니다.", exception);
        }
    }

    private static byte[] sha256(String value) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }

    private static String base64Url(String value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value.getBytes(StandardCharsets.UTF_8));
    }

    private String toHex(byte[] value) {
        StringBuilder result = new StringBuilder(value.length * 2);
        for (byte item : value) {
            result.append(String.format("%02x", item));
        }
        return result.toString();
    }

    public record JwtPair(String accessToken, String refreshToken, Instant accessExpiresAt, Instant refreshExpiresAt) {
    }

    public record JwtClaims(String accessId, String tokenId, String type, Instant expiresAt) {
    }
}
