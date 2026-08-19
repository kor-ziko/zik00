package com.zik00.shop.dto.notice;

import com.zik00.shop.domain.notice.Notice;

import java.time.LocalDateTime;

public record NoticeDetailResponse(
        long id,
        String category,
        String title,
        String content,
        boolean pinned,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public static NoticeDetailResponse from(Notice notice) {
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getCategory(),
                notice.getTitle(),
                notice.getContent(),
                notice.isPinned(),
                notice.getPublishedAt(),
                notice.getUpdatedAt()
        );
    }
}
