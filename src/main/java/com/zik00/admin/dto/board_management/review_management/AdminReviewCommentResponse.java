package com.zik00.admin.dto.board_management.review_management;

import com.zik00.admin.domain.board_management.review_management.AdminReviewComment;

import java.time.LocalDateTime;

public record AdminReviewCommentResponse(
        long id,
        long adminId,
        String adminName,
        String content,
        LocalDateTime createdAt
) {
    public static AdminReviewCommentResponse from(AdminReviewComment comment) {
        return new AdminReviewCommentResponse(
                comment.getId(), comment.getAdminId(), comment.getAdminName(),
                comment.getContent(), comment.getCreatedAt()
        );
    }
}
