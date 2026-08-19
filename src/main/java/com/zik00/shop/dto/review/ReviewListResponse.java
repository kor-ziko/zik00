package com.zik00.shop.dto.review;

import java.util.List;

public record ReviewListResponse(
        List<ReviewItemResponse> items,
        double averageRating,
        long totalCount,
        List<RatingCountResponse> ratingCounts,
        int page,
        int size,
        int totalPages
) {
}
