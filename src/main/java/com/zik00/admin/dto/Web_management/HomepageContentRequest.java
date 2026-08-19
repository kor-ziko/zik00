package com.zik00.admin.dto.Web_management;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public record HomepageContentRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 300) String subtitle,
        @Size(max = 20_000) String content,
        @Size(max = 1000) String imageUrl,
        @Size(max = 1000) String linkUrl,
        @Size(max = 100) String linkLabel,
        @Size(max = 30) String applicationType,
        @Min(0) int displayOrder,
        boolean active,
        LocalDateTime startsAt,
        LocalDateTime endsAt
) {
}
