package com.zik00.shop.domain.review;

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
@Table(name = "service_reviews")
public class ServiceReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "author_name", nullable = false, length = 100)
    private String authorName;

    @Column(nullable = false, length = 200)
    private String title;

    @Lob
    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(nullable = false)
    private int rating;

    @Column(name = "product_name", nullable = false, length = 200)
    private String productName;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private boolean featured;

    @Column(nullable = false)
    private boolean published;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected ServiceReview() {
    }

    public ServiceReview(
            String authorName,
            String title,
            String content,
            int rating,
            String productName,
            String imageUrl,
            boolean featured,
            boolean published
    ) {
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.rating = rating;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.featured = featured;
        this.published = published;
    }

    public void update(
            String authorName,
            String title,
            String content,
            int rating,
            String productName,
            String imageUrl,
            boolean featured,
            boolean published
    ) {
        this.authorName = authorName;
        this.title = title;
        this.content = content;
        this.rating = rating;
        this.productName = productName;
        this.imageUrl = imageUrl;
        this.featured = featured;
        this.published = published;
    }
}
