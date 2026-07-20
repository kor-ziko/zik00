package com.zik00.shop.service.auth;

public class RegistrationTermsRequiredException extends RuntimeException {
    public RegistrationTermsRequiredException() {
        super("필수 약관 동의가 필요합니다.");
    }
}
