package com.zik00.admin.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AdminCouponTemplateCreateRequest(
        @NotBlank(message = "쿠폰명을 입력해주세요.")
        String couponName,
        @NotBlank(message = "할인 타입을 선택해주세요.")
        String discountType,
        @Min(value = 0, message = "할인값은 0 이상이어야 합니다.")
        int discountValue,
        @Min(value = 0, message = "최소 주문 금액은 0 이상이어야 합니다.")
        int minimumOrderAmount,
        LocalDate startedDate,
        LocalDate expiredDate,
        @NotBlank(message = "발급 대상을 선택해주세요.")
        String targetType,
        boolean active
) {
}
