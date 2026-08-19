package com.zik00.admin.dto.board_management.notice_management;

import com.zik00.shop.domain.notice.Notice;

import java.time.LocalDateTime;

public record AdminNoticeResponse(
        long id,
        String category,
        String title,
        String content,
        boolean pinned,
        boolean published,
        LocalDateTime publishedAt,
        LocalDateTime updatedAt
) {
    public static AdminNoticeResponse from(Notice notice) {
        return new AdminNoticeResponse(
                notice.getId(), notice.getCategory(), notice.getTitle(), notice.getContent(),
                notice.isPinned(), notice.isPublished(), notice.getPublishedAt(), notice.getUpdatedAt()
        );
    }
}
