package com.zik00.shop.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.security.GeneralSecurityException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Base64;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class JwtServiceTest {
    private static final KeyPair KEY_PAIR = generateKeyPair();

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            Base64.getEncoder().encodeToString(KEY_PAIR.getPrivate().getEncoded()),
            Base64.getEncoder().encodeToString(KEY_PAIR.getPublic().getEncoded()),
            Duration.ofMinutes(15),
            Duration.ofDays(14),
            "zik00-test"
    );

    @Test
    void issuesAndValidatesAccessAndRefreshTokens() {
        JwtService.JwtPair pair = jwtService.issue("access-id-123");

        assertEquals("access-id-123", jwtService.validate(pair.accessToken(), JwtService.ACCESS).accessId());
        assertEquals("access-id-123", jwtService.validate(pair.refreshToken(), JwtService.REFRESH).accessId());
        assertNotEquals(pair.accessToken(), pair.refreshToken());
    }

    @Test
    void rejectsTokenWhenTypeDoesNotMatch() {
        JwtService.JwtPair pair = jwtService.issue("access-id-123");

        assertThrows(
                InvalidJwtException.class,
                () -> jwtService.validate(pair.refreshToken(), JwtService.ACCESS)
        );
    }

    @Test
    void rejectsTamperedToken() {
        String token = jwtService.issue("access-id-123").accessToken();
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("a") ? "b" : "a");

        assertThrows(InvalidJwtException.class, () -> jwtService.validate(tampered, JwtService.ACCESS));
    }

    private static KeyPair generateKeyPair() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2_048);
            return generator.generateKeyPair();
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
