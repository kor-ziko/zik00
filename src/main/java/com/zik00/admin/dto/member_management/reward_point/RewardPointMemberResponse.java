package com.zik00.admin.dto.member_management.reward_point;

public record RewardPointMemberResponse(Long memberId, String name, String nickname, String loginId, int balance) {
}
