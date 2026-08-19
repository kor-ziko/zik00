package com.zik00.admin.dto.board_management.one_to_one_inquiry;

public record AdminInquirySummaryResponse(
        long inquiryId,
        long memberId,
        String memberName,
        String title,
        boolean answered,
        String createdAt,
        long commentCount,
        long imageCount
) {
}
