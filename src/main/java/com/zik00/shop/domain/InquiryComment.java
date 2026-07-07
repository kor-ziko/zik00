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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "inquiry_id")
    private long inquiryId;

    @Column(name = "user_id")
    private long memberId;

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
        this.writerName = writerName;
        this.content = content;
        this.createdAt = createdAt;
    }

    public long getCommentId() {
        return commentId == null ? 0L : commentId;
    }

    public long getInquiryId() {
        return inquiryId;
    }

    public long getMemberId() {
        return memberId;
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
