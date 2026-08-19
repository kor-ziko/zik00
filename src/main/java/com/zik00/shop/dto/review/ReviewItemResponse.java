package com.zik00.shop.dto.review;

import com.zik00.shop.domain.review.ServiceReview;

import java.time.LocalDateTime;

public record ReviewItemResponse(
        long id,
        String authorName,
        String title,
        String content,
        int rating,
        String productName,
        String imageUrl,
        boolean featured,
        LocalDateTime createdAt
) {
    public static ReviewItemResponse from(ServiceReview review) {
        return new ReviewItemResponse(
                review.getId(),
                review.getAuthorName(),
                review.getTitle(),
                review.getContent(),
                review.getRating(),
                review.getProductName(),
                review.getImageUrl(),
                review.isFeatured(),
                review.getCreatedAt()
        );
    }
}
