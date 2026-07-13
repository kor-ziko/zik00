package com.zik00.admin.dto;

public record AdminSessionResponse(
        Long adminId,
        String loginId,
        String name
) {
    public static AdminSessionResponse from(AdminSession session) {
        return new AdminSessionResponse(session.adminId(), session.loginId(), session.name());
    }
}
