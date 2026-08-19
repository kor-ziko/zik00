package com.zik00.admin.dto.member_management.reward_point;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record RewardPointAdjustmentRequest(
        @NotNull Long memberId,
        int amount,
        @NotBlank String reason
) {
}
