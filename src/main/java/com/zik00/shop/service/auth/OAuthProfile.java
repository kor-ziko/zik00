package com.zik00.shop.service.auth;

public record OAuthProfile(String provider, String subject, String email, String displayName) {
    public OAuthProfile {
        provider = OAuthProvider.from(provider).registrationId();
        subject = normalizeRequired(subject, "OAuth 계정 식별자가 없습니다.");
        email = normalize(email);
        displayName = normalize(displayName);
    }

    public boolean hasValidEmail() {
        int at = email.indexOf('@');
        return email.length() <= 255 && at > 0 && at < email.length() - 1;
    }

    public String loginId() {
        return OAuthProvider.from(provider).loginId(subject);
    }

    private static String normalizeRequired(String value, String message) {
        String normalized = normalize(value);
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException(message);
        }
        return normalized;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
