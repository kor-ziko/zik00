package com.zik00.shop.dto.notice;

import com.zik00.shop.domain.notice.Notice;

import java.time.LocalDateTime;

public record NoticeSummaryResponse(
        long id,
        String category,
        String title,
        boolean pinned,
        LocalDateTime publishedAt
) {
    public static NoticeSummaryResponse from(Notice notice) {
        return new NoticeSummaryResponse(
                notice.getId(),
                notice.getCategory(),
                notice.getTitle(),
                notice.isPinned(),
                notice.getPublishedAt()
        );
    }
}
