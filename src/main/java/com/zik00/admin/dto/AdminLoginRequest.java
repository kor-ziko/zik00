package com.zik00.admin.dto;

import jakarta.validation.constraints.NotBlank;

// @dev - 관리자 로그인 데이터 처리
public record AdminLoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
