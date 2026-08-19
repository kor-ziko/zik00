package com.zik00.admin.domain.board_management.review_management;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "service_review_comments")
public class AdminReviewComment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "comment_id")
    private Long id;

    @Column(name = "review_id", nullable = false)
    private long reviewId;

    @Column(name = "admin_id", nullable = false)
    private long adminId;

    @Column(name = "admin_name", nullable = false, length = 100)
    private String adminName;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected AdminReviewComment() {
    }

    public AdminReviewComment(long reviewId, long adminId, String adminName, String content) {
        this.reviewId = reviewId;
        this.adminId = adminId;
        this.adminName = adminName;
        this.content = content;
        this.createdAt = LocalDateTime.now();
    }
}
