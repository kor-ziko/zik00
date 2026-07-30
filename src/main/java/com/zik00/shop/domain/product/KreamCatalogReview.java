package com.zik00.shop.domain.product;

import java.util.List;

public record KreamCatalogReview(
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
