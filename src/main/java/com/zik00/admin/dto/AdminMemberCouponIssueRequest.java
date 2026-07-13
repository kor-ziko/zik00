package com.zik00.admin.dto;

import java.time.LocalDate;
import java.util.List;

public record AdminMemberCouponIssueRequest(
        Long couponTemplateId,
        List<Long> memberIds,
        String couponName,
        String discountType,
        int discountValue,
        int minimumOrderAmount,
        LocalDate startedDate,
        LocalDate expiredDate
) {
}
