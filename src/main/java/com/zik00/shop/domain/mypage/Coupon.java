package com.zik00.shop.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "coupon", indexes = {
        @Index(name = "idx_coupon_member_period", columnList = "user_id, started_date, expired_date, coupon_id")
})
public class Coupon {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "coupon_id")
    private Long couponId;

    @Column(name = "coupon_template_id")
    private Long couponTemplateId;

    @Column(name = "user_id")
    private Long memberId;

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

    @Column(name = "issued_at")
    private LocalDateTime issuedAt;

    private boolean used;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "guest_identifier")
    private String guestIdentifier;

    protected Coupon() {
    }

    public Coupon(
            long couponId,
            long memberId,
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            LocalDate startedDate,
            LocalDate expiredDate,
            boolean used
    ) {
        this.couponId = couponId > 0 ? couponId : null;
        this.memberId = memberId;
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.startedDate = startedDate;
        this.expiredDate = expiredDate;
        this.used = used;
    }

    public static Coupon issueToMember(
            Long couponTemplateId,
            long memberId,
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            LocalDate startedDate,
            LocalDate expiredDate
    ) {
        Coupon coupon = new Coupon();
        coupon.couponTemplateId = couponTemplateId;
        coupon.memberId = memberId;
        coupon.couponName = normalize(couponName);
        coupon.discountType = normalize(discountType);
        coupon.discountValue = discountValue;
        coupon.minimumOrderAmount = minimumOrderAmount;
        coupon.startedDate = startedDate;
        coupon.expiredDate = expiredDate;
        coupon.issuedAt = LocalDateTime.now();
        coupon.used = false;
        return coupon;
    }

    public static Coupon issueToGuest(
            Long couponTemplateId,
            String guestIdentifier,
            String couponCode,
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            LocalDate startedDate,
            LocalDate expiredDate
    ) {
        Coupon coupon = new Coupon();
        coupon.couponTemplateId = couponTemplateId;
        coupon.guestIdentifier = normalize(guestIdentifier);
        coupon.couponCode = normalize(couponCode);
        coupon.couponName = normalize(couponName);
        coupon.discountType = normalize(discountType);
        coupon.discountValue = discountValue;
        coupon.minimumOrderAmount = minimumOrderAmount;
        coupon.startedDate = startedDate;
        coupon.expiredDate = expiredDate;
        coupon.issuedAt = LocalDateTime.now();
        coupon.used = false;
        return coupon;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    public long getCouponId() {
        return couponId == null ? 0L : couponId;
    }
    public long getCouponTemplateId() {
        return couponTemplateId == null ? 0L : couponTemplateId;
    }
    public long getMemberId() {
        return memberId == null ? 0L : memberId;
    }
}
