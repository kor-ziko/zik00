package com.zik00.shop.dto;

import com.zik00.shop.domain.Inquiry;
import lombok.Getter;

@Getter
public class InquiryView {
    private final long inquiryId;
    private final String title;
    private final String content;
    private final boolean status;
    private final String createdAt;

    private InquiryView(
            long inquiryId,
            String title,
            String content,
            boolean status,
            String createdAt
    ) {
        this.inquiryId = inquiryId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    public static InquiryView from(Inquiry inquiry) {
        return new InquiryView(
                inquiry.getInquiryId(),
                inquiry.getTitle(),
                inquiry.getContent(),
                inquiry.isStatus(),
                inquiry.getCreatedAt()
        );
    }
}
