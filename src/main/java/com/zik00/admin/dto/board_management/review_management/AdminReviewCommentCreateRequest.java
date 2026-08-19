package com.zik00.admin.dto.board_management.review_management;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminReviewCommentCreateRequest(
        @NotBlank @Size(max = 2_000) String content
) {
}
