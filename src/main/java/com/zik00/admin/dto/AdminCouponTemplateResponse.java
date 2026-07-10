package com.zik00.admin.dto;

import com.zik00.admin.domain.CouponTemplate;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record AdminCouponTemplateResponse(
        long id,
        String couponName,
        String discountType,
        int discountValue,
        int minimumOrderAmount,
        LocalDate startedDate,
        LocalDate expiredDate,
        String targetType,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static AdminCouponTemplateResponse from(CouponTemplate template) {
        return new AdminCouponTemplateResponse(
                template.getId(),
                template.getCouponName(),
                template.getDiscountType(),
                template.getDiscountValue(),
                template.getMinimumOrderAmount(),
                template.getStartedDate(),
                template.getExpiredDate(),
                template.getTargetType(),
                template.isActive(),
                template.getCreatedAt(),
                template.getUpdatedAt()
        );
    }
}
