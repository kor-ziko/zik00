package com.zik00.shop.dto;

import java.util.List;

import com.zik00.shop.domain.Inquiry;
import com.zik00.shop.domain.InquiryComment;

public class InquiryThread {
    private final Inquiry inquiry;
    private final List<InquiryComment> comments;

    public InquiryThread(Inquiry inquiry, List<InquiryComment> comments) {
        this.inquiry = inquiry;
        this.comments = comments;
    }

    public Inquiry getInquiry() {
        return inquiry;
    }
    public List<InquiryComment> getComments() {
        return comments;
    }
}
