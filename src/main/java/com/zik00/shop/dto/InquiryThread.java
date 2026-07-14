package com.zik00.shop.dto;

import java.util.List;

import lombok.Getter;

@Getter
public class InquiryThread {
    private final InquiryView inquiry;
    // @Suil - 댓글과 관리자 답변 사진을 묶은 화면 전용 데이터 사용
    private final List<InquiryCommentView> comments;
    private final List<InquiryImageView> images;

    public InquiryThread(InquiryView inquiry, List<InquiryCommentView> comments, List<InquiryImageView> images) {
        this.inquiry = inquiry;
        this.comments = comments;
        this.images = images;
    }

}
