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
@Table(name = "inquiries", indexes = {
        @Index(name = "idx_inquiries_member_id", columnList = "user_id, inquiry_id")
})
public class Inquiry {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inquiry_id")
    private Long inquiryId;

    @Column(name = "user_id")
    private long memberId;

    private String title;

    @Lob
    private String content;

    // @Suil - 문의의 답변 전후 상태를 boolean으로 관리
    private boolean status;
    @Column(name = "created_at")
    private String createdAt;

    protected Inquiry() {
    }

    public Inquiry(
            long inquiryId,
            long memberId,
            String title,
            String content,
            boolean status,
            String createdAt
    ) {
        this.inquiryId = inquiryId > 0 ? inquiryId : null;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
    }

    public boolean isStatus() {
        return status;
    }

    public void markAnswered() {
        this.status = true;
    }

    // @Suil - 사용자가 추가 댓글을 남기면 다시 답변대기로 변경
    public void markPending() {
        this.status = false;
    }

    public long getInquiryId() {
        return inquiryId == null ? 0L : inquiryId;
    }
    public long getMemberId() {
        return memberId;
    }
    public String getTitle() {
        return title;
    }
    public String getContent() {
        return content;
    }
    public String getCreatedAt() {
        return createdAt;
    }
}
