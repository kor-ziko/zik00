package com.zik00.admin.dto.board_management.one_to_one_inquiry;

import java.util.List;

public record AdminInquiryCommentResponse(
        long commentId,
        String writerType,
        String writerName,
        String content,
        String createdAt,
        List<AdminInquiryImageResponse> images
) {
}
