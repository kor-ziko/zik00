package com.zik00.admin.dto.board_management.notice_management;

import com.zik00.admin.domain.board_management.notice_management.NoticeCategory;

public record AdminNoticeCategoryResponse(long id, String name, int displayOrder) {
    public static AdminNoticeCategoryResponse from(NoticeCategory category) {
        return new AdminNoticeCategoryResponse(category.getId(), category.getName(), category.getDisplayOrder());
    }
}
