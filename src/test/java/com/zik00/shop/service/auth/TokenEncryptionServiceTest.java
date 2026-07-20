package com.zik00.shop.service.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class TokenEncryptionServiceTest {
    private final TokenEncryptionService encryptionService =
            new TokenEncryptionService("test-only-encryption-key-material");

    @Test
    void encryptsAndDecryptsAccessToken() {
        String accessToken = "google-access-token-value";

        String encrypted = encryptionService.encrypt(accessToken);

        assertNotEquals(accessToken, encrypted);
        assertEquals(accessToken, encryptionService.decrypt(encrypted));
    }

    @Test
    void usesDifferentNonceForEveryEncryption() {
        String accessToken = "same-access-token";

        assertNotEquals(
                encryptionService.encrypt(accessToken),
                encryptionService.encrypt(accessToken)
        );
    }
}
