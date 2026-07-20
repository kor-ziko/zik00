package com.zik00.shop.service.auth;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JwtService {
    public static final String ACCESS = "access";
    public static final String REFRESH = "refresh";
    private static final String ALGORITHM = "RS256";
    private static final String HEADER = base64Url("{\"alg\":\"RS256\",\"typ\":\"JWT\"}");

    private final ObjectMapper objectMapper;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final Duration accessTtl;
    private final Duration refreshTtl;
    private final String issuer;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${shop.jwt.private-key}") String privateKeyValue,
            @Value("${shop.jwt.public-key}") String publicKeyValue,
            @Value("${shop.jwt.access-token-ttl:PT15M}") Duration accessTtl,
            @Value("${shop.jwt.refresh-token-ttl:P14D}") Duration refreshTtl,
            @Value("${shop.jwt.issuer:zik00-shop}") String issuer
    ) {
        this.objectMapper = objectMapper;
        this.privateKey = readPrivateKey(privateKeyValue);
        this.publicKey = readPublicKey(publicKeyValue);
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

            JsonNode header = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[0]));
            if (!ALGORITHM.equals(header.path("alg").asString())
                    || !"JWT".equals(header.path("typ").asString())) {
                throw new InvalidJwtException("RS256 알고리즘으로 서명된 JWT만 사용할 수 있습니다.");
            }

            if (!verify(parts[0] + "." + parts[1], Base64.getUrlDecoder().decode(parts[2]))) {
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
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(value.getBytes(StandardCharsets.UTF_8));
            return signature.sign();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("JWT 서명에 실패했습니다.", exception);
        }
    }

    private boolean verify(String value, byte[] signatureValue) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(value.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureValue);
        } catch (GeneralSecurityException exception) {
            throw new InvalidJwtException("JWT 서명을 검증할 수 없습니다.", exception);
        }
    }

    private static PrivateKey readPrivateKey(String value) {
        try {
            byte[] encoded = decodeKey(value, "PRIVATE KEY");
            return KeyFactory.getInstance("RSA").generatePrivate(new PKCS8EncodedKeySpec(encoded));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_PRIVATE_KEY는 PKCS#8 형식의 RSA 개인키여야 합니다.", exception);
        }
    }

    private static PublicKey readPublicKey(String value) {
        try {
            byte[] encoded = decodeKey(value, "PUBLIC KEY");
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(encoded));
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("JWT_PUBLIC_KEY는 X.509 형식의 RSA 공개키여야 합니다.", exception);
        }
    }

    private static byte[] decodeKey(String value, String keyType) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("JWT RSA key is empty");
        }
        String normalized = value.replace("\\n", "\n")
                .replace("-----BEGIN " + keyType + "-----", "")
                .replace("-----END " + keyType + "-----", "")
                .replaceAll("\\s", "");
        return Base64.getDecoder().decode(normalized);
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
