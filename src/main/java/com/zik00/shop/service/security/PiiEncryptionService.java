package com.zik00.shop.service.security;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class PiiEncryptionService {
    private static final String ENCRYPTED_PREFIX = "enc:";
    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final String KEY_ALGORITHM = "AES";
    private static final int AES_256_KEY_BYTES = 32;
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final byte[] AAD = "zik00:pii:v1".getBytes(StandardCharsets.UTF_8);

    private final String currentKeyVersion;
    private final Map<String, SecretKey> keys;
    private final SecureRandom secureRandom = new SecureRandom();

    public PiiEncryptionService(
            @Value("${shop.pii.current-key-version:v1}") String currentKeyVersion,
            @Value("${shop.pii.encryption-keys}") String configuredKeys
    ) {
        this.currentKeyVersion = requireVersion(currentKeyVersion);
        this.keys = parseKeys(configuredKeys);
        if (!keys.containsKey(this.currentKeyVersion)) {
            throw new IllegalStateException("Current PII encryption key version is not configured.");
        }
    }

    public String encrypt(String plaintext) {
        if (plaintext == null || plaintext.isEmpty()) {
            return plaintext;
        }

        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, keys.get(currentKeyVersion), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            byte[] payload = ByteBuffer.allocate(iv.length + ciphertext.length)
                    .put(iv)
                    .put(ciphertext)
                    .array();
            return ENCRYPTED_PREFIX + currentKeyVersion + ":"
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(payload);
        } catch (GeneralSecurityException exception) {
            throw new IllegalStateException("PII encryption failed.", exception);
        }
    }

    public String decrypt(String storedValue) {
        if (storedValue == null || storedValue.isEmpty() || !isEncrypted(storedValue)) {
            return storedValue;
        }

        String[] parts = storedValue.split(":", 3);
        if (parts.length != 3 || parts[1].isBlank() || parts[2].isBlank()) {
            throw new IllegalStateException("Encrypted PII value has an invalid format.");
        }
        SecretKey key = keys.get(parts[1]);
        if (key == null) {
            throw new IllegalStateException("PII encryption key version is not available: " + parts[1]);
        }

        try {
            byte[] payload = Base64.getUrlDecoder().decode(parts[2]);
            if (payload.length <= IV_BYTES) {
                throw new IllegalStateException("Encrypted PII payload is too short.");
            }
            byte[] iv = Arrays.copyOfRange(payload, 0, IV_BYTES);
            byte[] ciphertext = Arrays.copyOfRange(payload, IV_BYTES, payload.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(ciphertext), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new IllegalStateException("PII decryption failed.", exception);
        }
    }

    public boolean isEncrypted(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX);
    }

    public boolean isEncryptedWithCurrentKey(String value) {
        return value != null && value.startsWith(ENCRYPTED_PREFIX + currentKeyVersion + ":");
    }

    private Map<String, SecretKey> parseKeys(String configuredKeys) {
        if (configuredKeys == null || configuredKeys.isBlank()) {
            throw new IllegalStateException("PII encryption keys must not be blank.");
        }

        Map<String, SecretKey> parsedKeys = new LinkedHashMap<>();
        for (String entry : configuredKeys.split(",")) {
            String[] parts = entry.trim().split(":", 2);
            if (parts.length != 2) {
                throw new IllegalStateException("PII encryption key must use version:base64 format.");
            }
            String version = requireVersion(parts[0]);
            byte[] keyBytes;
            try {
                keyBytes = Base64.getDecoder().decode(parts[1].trim());
            } catch (IllegalArgumentException exception) {
                throw new IllegalStateException("PII encryption key is not valid Base64.", exception);
            }
            if (keyBytes.length != AES_256_KEY_BYTES) {
                throw new IllegalStateException("PII encryption key must be exactly 256 bits.");
            }
            if (parsedKeys.putIfAbsent(version, new SecretKeySpec(keyBytes, KEY_ALGORITHM)) != null) {
                throw new IllegalStateException("Duplicate PII encryption key version: " + version);
            }
        }
        return Map.copyOf(parsedKeys);
    }

    private String requireVersion(String value) {
        String version = value == null ? "" : value.trim();
        if (!version.matches("[A-Za-z0-9_-]{1,20}")) {
            throw new IllegalStateException("PII encryption key version is invalid.");
        }
        return version;
    }
}
