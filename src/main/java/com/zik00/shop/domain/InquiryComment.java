package com.zik00.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

@Entity
@Table(name = "inquiry_comments", indexes = {
        @Index(name = "idx_inquiry_comments_inquiry_comment", columnList = "inquiry_id, comment_id")
})
public class InquiryComment {
    // @Suil - 사용자 댓글과 관리자 답변 작성자를 구분
    public static final String USER_WRITER_TYPE = "USER";
    public static final String ADMIN_WRITER_TYPE = "ADMIN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "inquiry_id")
    private long inquiryId;

    @Column(name = "user_id")
    private Long memberId;

    @Column(name = "admin_id")
    private Long adminId;

    @Column(name = "writer_type", nullable = false, length = 20)
    private String writerType;

    @Column(name = "writer_name")
    private String writerName;

    @Lob
    private String content;

    @Column(name = "created_at")
    private String createdAt;

    protected InquiryComment() {
    }

    public InquiryComment(
            long commentId,
            long inquiryId,
            long memberId,
            String writerName,
            String content,
            String createdAt
    ) {
        this.commentId = commentId > 0 ? commentId : null;
        this.inquiryId = inquiryId;
        this.memberId = memberId;
        this.writerType = USER_WRITER_TYPE;
        this.writerName = writerName;
        this.content = content;
        this.createdAt = createdAt;
    }

    // @Suil - 관리자 답변 관련 처리 기능 넣음
    public static InquiryComment adminReply(
            long inquiryId,
            long adminId,
            String writerName,
            String content,
            String createdAt
    ) {
        InquiryComment comment = new InquiryComment();
        comment.inquiryId = inquiryId;
        comment.adminId = adminId;
        comment.writerType = ADMIN_WRITER_TYPE;
        comment.writerName = writerName;
        comment.content = content;
        comment.createdAt = createdAt;
        return comment;
    }

    public long getCommentId() {
        return commentId == null ? 0L : commentId;
    }
    public long getInquiryId() {
        return inquiryId;
    }
    public long getMemberId() {
        return memberId == null ? 0L : memberId;
    }
    public long getAdminId() {
        return adminId == null ? 0L : adminId;
    }
    public String getWriterType() {
        return writerType;
    }
    public String getWriterName() {
        return writerName;
    }
    public String getContent() {
        return content;
    }
    public String getCreatedAt() {
        return createdAt;
    }
}
