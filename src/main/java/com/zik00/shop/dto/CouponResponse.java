package com.zik00.shop.dto;

import java.time.LocalDate;

import com.zik00.shop.domain.Coupon;
import lombok.Getter;

@Getter
public class CouponResponse {
    private final String couponName;
    private final String discountType;
    private final int discountValue;
    private final int minimumOrderAmount;
    private final LocalDate startedDate;
    private final LocalDate expiredDate;
    private final boolean used;

    private CouponResponse(
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            LocalDate startedDate,
            LocalDate expiredDate,
            boolean used
    ) {
        this.couponName = couponName;
        this.discountType = discountType;
        this.discountValue = discountValue;
        this.minimumOrderAmount = minimumOrderAmount;
        this.startedDate = startedDate;
        this.expiredDate = expiredDate;
        this.used = used;
    }

    public static CouponResponse from(Coupon coupon) {
        return new CouponResponse(
                coupon.getCouponName(),
                coupon.getDiscountType(),
                coupon.getDiscountValue(),
                coupon.getMinimumOrderAmount(),
                coupon.getStartedDate(),
                coupon.getExpiredDate(),
                coupon.isUsed()
        );
    }
}
