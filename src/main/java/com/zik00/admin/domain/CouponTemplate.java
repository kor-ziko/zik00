package com.zik00.admin.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@Entity
@Table(name = "coupon_template")
public class CouponTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_template_id")
    private Long id;

    @Column(name = "coupon_name")
    private String couponName;

    @Column(name = "discount_type")
    private String discountType;

    @Column(name = "discount_value")
    private int discountValue;

    @Column(name = "minimum_order_amount")
    private int minimumOrderAmount;

    @Column(name = "started_date")
    private LocalDate startedDate;

    @Column(name = "expired_date")
    private LocalDate expiredDate;

    @Column(name = "target_type")
    private String targetType;

    private boolean active;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private LocalDateTime updatedAt;

    protected CouponTemplate() {
    }

    public CouponTemplate(
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            LocalDate startedDate,
            LocalDate expiredDate,
            String targetType,
            boolean active
    ) {
        this.couponName = normalize(couponName);
        this.discountType = normalize(discountType);
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.startedDate = startedDate;
        this.expiredDate = expiredDate;
        this.targetType = normalize(targetType).isBlank() ? "ALL" : normalize(targetType);
        this.active = active;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public long getId() {
        return id == null ? 0L : id;
    }
}
