package com.zik00.shop.domain;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

@Entity
@Table(name = "coupon", indexes = {
        @Index(name = "idx_coupon_member_expired", columnList = "user_id, expired_date, coupon_id")
})
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "user_id")
    private long memberId;

    @Column(name = "coupon_name")
    private String couponName;

    @Column(name = "discount_type")
    private String discountType;

    @Column(name = "discount_value")
    private int discountValue;

    @Column(name = "minimum_order_amount")
    private int minimumOrderAmount;

    @Column(name = "expired_date")
    private LocalDate expiredDate;

    private boolean used;

    protected Coupon() {
    }

    public Coupon(
            long couponId,
            long memberId,
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            LocalDate expiredDate,
            boolean used
    ) {
        this.couponId = couponId > 0 ? couponId : null;
        this.memberId = memberId;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.expiredDate = expiredDate;
        this.used = used;
    }

    public long getCouponId() {
        return couponId == null ? 0L : couponId;
    }

    public long getMemberId() {
        return memberId;
    }

    public String getCouponName() {
        return couponName;
    }

    public String getDiscountType() {
        return discountType;
    }

    public int getDiscountValue() {
        return discountValue;
    }

    public int getMinimumOrderAmount() {
        return minimumOrderAmount;
    }

    public LocalDate getExpiredDate() {
        return expiredDate;
    }

    public boolean isUsed() {
        return used;
    }
}
