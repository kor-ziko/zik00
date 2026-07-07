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

    private String status;
    @Column(name = "created_at")
    private String createdAt;

    protected Inquiry() {
    }

    public Inquiry(
            long inquiryId,
            long memberId,
            String title,
            String content,
            String status,
            String createdAt
    ) {
        this.inquiryId = inquiryId > 0 ? inquiryId : null;
        this.memberId = memberId;
        this.title = title;
        this.content = content;
        this.status = status;
        this.createdAt = createdAt;
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

    public String getStatus() {
        return status;
    }

    public String getCreatedAt() {
        return createdAt;
    }
}
