package com.zik00.shop.dto.mypage;

import java.util.List;

import com.zik00.shop.domain.InquiryComment;
import lombok.Getter;

// @Suil - 사용자 화면에 댓글 작성자 유형과 답변 사진을 함께 전달 (굳이 필요없다 생각하면 지워도 됨)

@Getter
public class InquiryCommentView {
    private final long commentId;
    private final String writerName;
    private final String writerType;
    private final String content;
    private final String createdAt;
    private final List<InquiryImageView> images;

    public InquiryCommentView(InquiryComment comment, List<InquiryImageView> images) {
        this.commentId = comment.getCommentId();
        this.writerName = comment.getWriterName();
        this.writerType = comment.getWriterType();
        this.content = comment.getContent();
        this.createdAt = comment.getCreatedAt();
        this.images = images;
    }

    public boolean isAdmin() {
        return InquiryComment.ADMIN_WRITER_TYPE.equals(writerType);
    }
}
