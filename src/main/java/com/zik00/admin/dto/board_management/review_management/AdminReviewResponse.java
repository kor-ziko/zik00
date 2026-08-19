package com.zik00.admin.dto.board_management.review_management;

import com.zik00.shop.domain.review.ServiceReview;

import java.time.LocalDateTime;
import java.util.List;

public record AdminReviewResponse(
        long id,
        String authorName,
        String title,
        String content,
        int rating,
        String productName,
        String imageUrl,
        boolean featured,
        boolean published,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<AdminReviewCommentResponse> comments
) {
    public static AdminReviewResponse from(
            ServiceReview review,
            List<AdminReviewCommentResponse> comments
    ) {
        return new AdminReviewResponse(
                review.getId(), review.getAuthorName(), review.getTitle(), review.getContent(),
                review.getRating(), review.getProductName(), review.getImageUrl(),
                review.isFeatured(), review.isPublished(), review.getCreatedAt(),
                review.getUpdatedAt(), comments
        );
    }
}
