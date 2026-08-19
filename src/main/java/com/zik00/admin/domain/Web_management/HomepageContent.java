package com.zik00.admin.domain.Web_management;

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
@Table(name = "homepage_contents")
public class HomepageContent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long id;

    @Column(name = "content_type", nullable = false, length = 40)
    private String contentType;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 300)
    private String subtitle;

    @Lob
    @Column(columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "link_url", length = 1000)
    private String linkUrl;

    @Column(name = "link_label", length = 100)
    private String linkLabel;

    @Column(name = "application_type", length = 30)
    private String applicationType;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "starts_at")
    private LocalDateTime startsAt;

    @Column(name = "ends_at")
    private LocalDateTime endsAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected HomepageContent() {
    }

    public HomepageContent(String contentType, String title, String subtitle, String content,
                           String imageUrl, String linkUrl, String linkLabel, String applicationType, int displayOrder,
                           boolean active, LocalDateTime startsAt, LocalDateTime endsAt) {
        update(contentType, title, subtitle, content, imageUrl, linkUrl, linkLabel, applicationType,
                displayOrder, active, startsAt, endsAt);
    }

    public void update(String contentType, String title, String subtitle, String content,
                       String imageUrl, String linkUrl, String linkLabel, String applicationType, int displayOrder,
                       boolean active, LocalDateTime startsAt, LocalDateTime endsAt) {
        this.contentType = contentType;
        this.title = title;
        this.subtitle = subtitle;
        this.content = content;
        this.imageUrl = imageUrl;
        this.linkUrl = linkUrl;
        this.linkLabel = linkLabel;
        this.applicationType = applicationType;
        this.displayOrder = displayOrder;
        this.active = active;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
}
