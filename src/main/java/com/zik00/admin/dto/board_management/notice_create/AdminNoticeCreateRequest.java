package com.zik00.admin.dto.board_management.notice_create;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record AdminNoticeCreateRequest(
        @NotBlank @Size(max = 50) String category,
        @NotBlank @Size(max = 255) String title,
        @NotBlank @Size(max = 20_000) String content,
        boolean pinned,
        boolean published,
        LocalDateTime publishedAt
) {
}
