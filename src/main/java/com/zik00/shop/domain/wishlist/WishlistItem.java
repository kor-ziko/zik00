package com.zik00.shop.domain.wishlist;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "wishlist_items")
public class WishlistItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "wishlist_item_id")
    private Long id;

    @Column(name = "user_id", nullable = false)
    private long userId;

    @Column(name = "product_id", nullable = false, length = 255)
    private String productId;

    @Column(name = "product_name", nullable = false, length = 500)
    private String productName;

    @Column(length = 200)
    private String brand;

    @Column(name = "image_url", length = 1500)
    private String imageUrl;

    @Column(nullable = false)
    private long price;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    protected WishlistItem() {
    }

    public WishlistItem(long userId, String productId, String productName, String brand,
                        String imageUrl, long price, String currency, String sourceUrl) {
        this.userId = userId;
        updateSnapshot(productId, productName, brand, imageUrl, price, currency, sourceUrl);
    }

    public void updateSnapshot(String productId, String productName, String brand,
                               String imageUrl, long price, String currency, String sourceUrl) {
        this.productId = productId;
        this.productName = productName;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.price = price;
        this.currency = currency;
        this.sourceUrl = sourceUrl;
    }
}
