package com.zik00.admin.dto.member_management.reward_point;

import java.time.LocalDateTime;

public record RewardPointHistoryResponse(
        Long id, Long memberId, String memberName, String loginId, int amount,
        int balanceAfter, String reason, LocalDateTime createdAt
) {
}
