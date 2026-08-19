package com.zik00.shop.dto.notice;

import java.util.List;

public record NoticeListResponse(
        List<NoticeSummaryResponse> items,
        List<String> categories,
        int page,
        int size,
        long totalCount,
        int totalPages
) {
}
