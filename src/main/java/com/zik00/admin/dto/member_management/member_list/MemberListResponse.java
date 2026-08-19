package com.zik00.admin.dto.member_management.member_list;

import java.time.LocalDate;

public record MemberListResponse(
        Long id, String name, String nickname, String loginId, String email, String phone,
        String status, int completedOrderCount, int rewardPoint, int depositBalance, LocalDate joinedDate
) {
}
