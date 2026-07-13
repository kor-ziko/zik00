package com.zik00.admin.dto;

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
