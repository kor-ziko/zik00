package com.zik00.shop.dto.auth.registration_terms;

public record RegistrationTermResponse(
        String id,
        String stitle,
        String title,
        String content,
        String button,
        boolean required
) {
}
