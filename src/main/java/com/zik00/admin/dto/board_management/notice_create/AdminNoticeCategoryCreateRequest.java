package com.zik00.admin.dto.board_management.notice_create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminNoticeCategoryCreateRequest(
        @NotBlank @Size(max = 50) String name
) {
}
