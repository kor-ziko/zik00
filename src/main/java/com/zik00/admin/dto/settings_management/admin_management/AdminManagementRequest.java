package com.zik00.admin.dto.settings_management.admin_management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record AdminManagementRequest(
        @NotBlank
        @Size(min = 4, max = 50)
        @Pattern(regexp = "^[A-Za-z0-9._-]+$", message = "관리자 아이디는 영문, 숫자, 마침표, 밑줄, 하이픈만 사용할 수 있습니다.")
        String loginId,
        @NotBlank @Size(max = 100) String name,
        @Size(max = 100) String password,
        boolean active
) {}
