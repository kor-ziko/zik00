package com.zik00.shop.dto.search;

import java.util.List;

public record SearchResultResponse(
        String query,
        long totalCount,
        int page,
        int size,
        int totalPages,
        List<SearchProductResponse> items,
        List<SearchFacetResponse> categories,
        List<SearchFacetResponse> brands
) {
}
