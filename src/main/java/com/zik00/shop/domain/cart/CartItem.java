package com.zik00.shop.domain.cart;

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
@Table(name = "cart_items")
public class CartItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "cart_item_id")
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

    @Column(name = "unit_price", nullable = false)
    private long unitPrice;

    @Column(nullable = false, length = 10)
    private String currency;

    @Column(name = "source_url", length = 2000)
    private String sourceUrl;

    @Lob
    @Column(name = "option_data", nullable = false, columnDefinition = "LONGTEXT")
    private String optionData;

    @Column(name = "option_key", nullable = false, length = 64)
    private String optionKey;

    @Column(nullable = false)
    private int quantity;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected CartItem() {
    }

    public CartItem(long userId, String productId, String productName, String brand, String imageUrl,
                    long unitPrice, String currency, String sourceUrl, String optionData,
                    String optionKey, int quantity) {
        this.userId = userId;
        this.productId = productId;
        this.optionData = optionData;
        this.optionKey = optionKey;
        this.quantity = quantity;
        updateSnapshot(productName, brand, imageUrl, unitPrice, currency, sourceUrl);
    }

    public void updateSnapshot(String productName, String brand, String imageUrl,
                               long unitPrice, String currency, String sourceUrl) {
        this.productName = productName;
        this.brand = brand;
        this.imageUrl = imageUrl;
        this.unitPrice = unitPrice;
        this.currency = currency;
        this.sourceUrl = sourceUrl;
    }

    public void addQuantity(int amount) {
        this.quantity = Math.min(10, this.quantity + amount);
    }

    public void changeQuantity(int quantity) {
        this.quantity = quantity;
    }
}
