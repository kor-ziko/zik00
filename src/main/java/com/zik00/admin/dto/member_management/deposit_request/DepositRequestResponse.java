package com.zik00.admin.dto.member_management.deposit_request;

import java.time.LocalDateTime;

public record DepositRequestResponse(
        Long id, Long memberId, String memberName, String loginId, int amount,
        String depositorName, String status, String adminMemo,
        LocalDateTime requestedAt, LocalDateTime processedAt
) {
}
