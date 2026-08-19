package com.zik00.admin.dto.member_management.deposit_history;

import java.time.LocalDateTime;

public record DepositHistoryResponse(
        Long id, Long memberId, String memberName, String loginId, String transactionType,
        int amount, int balanceAfter, String description, LocalDateTime createdAt
) {
}
