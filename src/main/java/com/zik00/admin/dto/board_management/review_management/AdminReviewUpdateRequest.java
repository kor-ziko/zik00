package com.zik00.admin.dto.board_management.review_management;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReviewUpdateRequest(
        @NotBlank @Size(max = 100) String authorName,
        @NotBlank @Size(max = 200) String title,
        @NotBlank @Size(max = 20_000) String content,
        @Min(1) @Max(5) int rating,
        @NotBlank @Size(max = 200) String productName,
        @Size(max = 500) String imageUrl,
        boolean featured,
        boolean published
) {
}
