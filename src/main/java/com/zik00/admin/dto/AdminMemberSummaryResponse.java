package com.zik00.admin.dto;

import com.zik00.shop.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;

// @dev 유저 간락 정보 보이기
public record AdminMemberSummaryResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String loginId,
        String phone,
        String provider,
        String role,
        String status,
        int completedOrderCount,
        LocalDateTime createdAt
) {
    private static final String DEFAULT_PROVIDER = "LOCAL";
    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STATUS = "ACTIVE";

    public static AdminMemberSummaryResponse from(User user) {
        return new AdminMemberSummaryResponse(
                user.getMemberId(),
                user.getEmail(),
                user.getName(),
                user.getNickname(),
                user.getUserId(),
                phoneOf(user),
                DEFAULT_PROVIDER,
                DEFAULT_ROLE,
                DEFAULT_STATUS,
                user.getCompletedOrderCount(),
                toDateTime(user.getJoinedDate())
        );
    }

    private static String phoneOf(User user) {
        if (hasText(user.getMobilePhone())) {
            return user.getMobilePhone();
        }
        return user.getTelephone();
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private static LocalDateTime toDateTime(LocalDate date) {
        return date == null ? null : date.atStartOfDay();
    }
}
