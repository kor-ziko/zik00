package com.zik00.shop.service.payment;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

public final class SbPaymentSignature {
    private static final Charset SHIFT_JIS = Charset.forName("Shift_JIS");

    private SbPaymentSignature() {
    }

    public static String request(Map<String, String> fields, List<String> order, String hashKey) {
        return sha1(concatenate(fields, order, hashKey), StandardCharsets.UTF_8);
    }

    public static boolean responseMatches(
            Map<String, String> fields,
            List<String> order,
            String hashKey,
            String received
    ) {
        if (received == null || received.isBlank()) return false;
        String expected = sha1(concatenate(fields, order, hashKey), SHIFT_JIS);
        return MessageDigest.isEqual(
                expected.toUpperCase().getBytes(StandardCharsets.US_ASCII),
                received.toUpperCase().getBytes(StandardCharsets.US_ASCII)
        );
    }

    private static String concatenate(Map<String, String> fields, List<String> order, String hashKey) {
        StringBuilder value = new StringBuilder();
        order.forEach(name -> value.append(fields.getOrDefault(name, "")));
        return value.append(hashKey).toString();
    }

    private static String sha1(String value, Charset charset) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-1").digest(value.getBytes(charset));
            StringBuilder result = new StringBuilder(digest.length * 2);
            for (byte part : digest) result.append(String.format("%02x", part & 0xff));
            return result.toString();
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-1을 사용할 수 없습니다.", exception);
        }
    }
}
