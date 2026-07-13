package com.zik00.admin.dto;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AdminGuestCouponIssueRequest(
        Long couponTemplateId,
        @NotBlank(message = "비회원 식별값을 입력해주세요.")
        String guestIdentifier,
        String couponCode,
        String couponName,
        String discountType,
        int discountValue,
        int minimumOrderAmount,
        LocalDate startedDate,
        LocalDate expiredDate
) {
}
