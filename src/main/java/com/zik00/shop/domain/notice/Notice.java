package com.zik00.shop.domain.notice;

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
@Table(name = "notices")
public class Notice {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notice_id")
    private Long id;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(nullable = false, length = 255)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private boolean pinned;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "published_at", nullable = false)
    private LocalDateTime publishedAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected Notice() {
    }

    public Notice(
            String category,
            String title,
            String content,
            boolean pinned,
            boolean published,
            LocalDateTime publishedAt
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
        this.publishedAt = publishedAt;
    }

    public void update(
            String category,
            String title,
            String content,
            boolean pinned,
            boolean published,
            LocalDateTime publishedAt
    ) {
        this.category = category;
        this.title = title;
        this.content = content;
        this.pinned = pinned;
        this.published = published;
        this.publishedAt = publishedAt;
    }
}
