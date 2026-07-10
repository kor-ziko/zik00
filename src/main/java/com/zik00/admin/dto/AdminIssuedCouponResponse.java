package com.zik00.admin.dto;

import com.zik00.shop.domain.Coupon;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminIssuedCouponResponse(
        long id,
        long couponTemplateId,
        long memberId,
        String couponName,
        String discountType,
        int discountValue,
        int minimumOrderAmount,
        LocalDate startedDate,
        LocalDate expiredDate,
        boolean used,
        LocalDateTime issuedAt,
        LocalDateTime usedAt,
        String couponCode,
        String guestIdentifier
) {
    public static AdminIssuedCouponResponse from(Coupon coupon) {
        return new AdminIssuedCouponResponse(
                coupon.getCouponId(),
                coupon.getCouponTemplateId(),
                coupon.getMemberId(),
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getStartedDate(),
                coupon.getExpiredDate(),
                coupon.isUsed(),
                coupon.getIssuedAt(),
                coupon.getUsedAt(),
                coupon.getCouponCode(),
                coupon.getGuestIdentifier()
        );
    }
}
