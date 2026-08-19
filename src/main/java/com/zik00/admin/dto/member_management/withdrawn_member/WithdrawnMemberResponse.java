package com.zik00.admin.dto.member_management.withdrawn_member;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record WithdrawnMemberResponse(
        Long id, String name, String nickname, String loginId, String email,
        LocalDate joinedDate, LocalDateTime withdrawnAt, String memo
) {
}
