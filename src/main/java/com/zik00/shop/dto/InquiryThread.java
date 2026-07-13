package com.zik00.shop.dto;

import java.util.List;

import com.zik00.shop.domain.Inquiry;

public class InquiryThread {
    private final Inquiry inquiry;
    // @Suil - 댓글과 관리자 답변 사진을 묶은 화면 전용 데이터 사용
    private final List<InquiryCommentView> comments;
    private final List<InquiryImageView> images;

    public InquiryThread(Inquiry inquiry, List<InquiryCommentView> comments, List<InquiryImageView> images) {
        this.inquiry = inquiry;
        this.comments = comments;
        this.images = images;
    }

    public Inquiry getInquiry() {
        return inquiry;
    }
    public List<InquiryCommentView> getComments() {
        return comments;
    }
    public List<InquiryImageView> getImages() {
        return images;
    }
}
