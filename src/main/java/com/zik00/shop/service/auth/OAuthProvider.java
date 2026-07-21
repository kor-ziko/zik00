package com.zik00.shop.service.auth;

import java.util.Locale;

public enum OAuthProvider {
    GOOGLE("google", "sub"),
    KAKAO("kakao", "id"),
    LINE("line", "userId");

    private final String registrationId;
    private final String userIdentifierAttribute;

    OAuthProvider(String registrationId, String userIdentifierAttribute) {
        this.registrationId = registrationId;
        this.userIdentifierAttribute = userIdentifierAttribute;
    }

    public String registrationId() {
        return registrationId;
    }

    public String userIdentifierAttribute() {
        return userIdentifierAttribute;
    }

    public String loginId(String subject) {
        if (subject == null || subject.isBlank()) {
            throw new IllegalArgumentException("OAuth 계정 식별자가 없습니다.");
        }
        return registrationId + ":" + subject.trim();
    }

    public static OAuthProvider from(String value) {
        String normalized = value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
        for (OAuthProvider provider : values()) {
            if (provider.registrationId.equals(normalized)) {
                return provider;
            }
        }
        throw new IllegalArgumentException("지원하지 않는 OAuth 공급자입니다.");
    }
}
