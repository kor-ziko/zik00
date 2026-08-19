package com.zik00.admin.dto.board_management.one_to_one_inquiry;

import java.util.List;

public record AdminInquiryDetailResponse(
        long inquiryId,
        long memberId,
        String memberName,
        String memberNickname,
        String memberEmail,
        String title,
        String content,
        boolean answered,
        String createdAt,
        List<AdminInquiryImageResponse> images,
        List<AdminInquiryCommentResponse> comments
) {
}
