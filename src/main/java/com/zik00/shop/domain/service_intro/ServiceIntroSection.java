package com.zik00.shop.domain.service_intro;

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
@Table(name = "service_intro_sections")
public class ServiceIntroSection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "section_id")
    private Long id;

    @Column(name = "section_type", nullable = false, length = 40)
    private String sectionType;

    @Column(length = 100)
    private String eyebrow;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(length = 500)
    private String detail;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(nullable = false)
    private boolean active;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceIntroSection() {
    }

    public ServiceIntroSection(
            String sectionType,
            String eyebrow,
            String title,
            String content,
            String detail,
            String imageUrl,
            int displayOrder,
            boolean active
    ) {
        this.sectionType = sectionType;
        this.eyebrow = eyebrow;
        this.title = title;
        this.content = content;
        this.detail = detail;
        this.imageUrl = imageUrl;
        this.displayOrder = displayOrder;
        this.active = active;
    }
}
