package com.zik00.shop.dto;

import java.util.List;

import com.zik00.shop.domain.Inquiry;
import com.zik00.shop.domain.InquiryComment;

public class InquiryThread {
    private final Inquiry inquiry;
    private final List<InquiryComment> comments;
    private final List<InquiryImageView> images;

    public InquiryThread(Inquiry inquiry, List<InquiryComment> comments, List<InquiryImageView> images) {
        this.inquiry = inquiry;
        this.comments = comments;
        this.images = images;
    }

    public Inquiry getInquiry() {
        return inquiry;
    }
    public List<InquiryComment> getComments() {
        return comments;
    }
    public List<InquiryImageView> getImages() {
        return images;
    }
}
