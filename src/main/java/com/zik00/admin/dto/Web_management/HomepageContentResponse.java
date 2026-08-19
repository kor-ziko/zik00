package com.zik00.admin.dto.Web_management;

import com.zik00.admin.domain.Web_management.HomepageContent;

import java.time.LocalDateTime;

public record HomepageContentResponse(
        long id, String contentType, String title, String subtitle, String content,
        String imageUrl, String linkUrl, String linkLabel, String applicationType, int displayOrder, boolean active,
        LocalDateTime startsAt, LocalDateTime endsAt, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static HomepageContentResponse from(HomepageContent item) {
        return new HomepageContentResponse(item.getId(), item.getContentType(), item.getTitle(),
                item.getSubtitle(), item.getContent(), item.getImageUrl(), item.getLinkUrl(),
                item.getLinkLabel(), item.getApplicationType(), item.getDisplayOrder(), item.isActive(), item.getStartsAt(),
                item.getEndsAt(), item.getCreatedAt(), item.getUpdatedAt());
    }
}
