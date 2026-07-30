package com.zik00.shop.dto.product;

import java.util.List;

public record ProductReviewResponse(
        String reviewId,
        String reviewType,
        String author,
        String content,
        String createdAt,
        Double rating,
        int likeCount,
        List<String> images,
        String reviewUrl
) {
}
