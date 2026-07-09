package com.zik00.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank String loginId,
        @NotBlank String password
) {
}
