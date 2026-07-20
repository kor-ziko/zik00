package com.zik00.admin.dto;

import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.User;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.function.UnaryOperator;

// @dev - 유저 목록 클릭 시 전부 보이게 하는 것
// 현재 여기는 차라리 유저 더블클릭 시 새 창으로 보이게끔 하는건 어떤가 싶음. 가독성 떨어짐
public record AdminMemberDetailResponse(
        Long id,
        String email,
        String name,
        String nickname,
        String loginId,
        String phone,
        String telephone,
        String mobilePhone,
        LocalDate birthDate,
        String gender,
        String provider,
        String providerId,
        String role,
        String status,
        String memo,
        int depositBalance,
        int rewardPoint,
        int completedOrderCount,
        LocalDate joinedDate,
        boolean alarmConsent,
        List<AdminMemberAddressResponse> addresses,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    private static final String DEFAULT_PROVIDER = "LOCAL";
    private static final String DEFAULT_ROLE = "USER";
    private static final String DEFAULT_STATUS = "ACTIVE";

    public static AdminMemberDetailResponse from(
            User user,
            List<DeliveryAddress> addresses,
            UnaryOperator<String> decrypt
    ) {
        LocalDateTime joinedAt = toDateTime(user.getJoinedDate());

        return new AdminMemberDetailResponse(
                user.getMemberId(),
                decrypt.apply(user.getEmail()),
                decrypt.apply(user.getName()),
                user.getNickname(),
                user.getUserId(),
                decrypt.apply(phoneOf(user)),
                decrypt.apply(user.getTelephone()),
                decrypt.apply(user.getMobilePhone()),
                user.getBirthDate(),
                decrypt.apply(user.getGender()),
                DEFAULT_PROVIDER,
                user.getUserId(),
                DEFAULT_ROLE,
                DEFAULT_STATUS,
                user.getMemberDetail(),
                user.getDepositBalance(),
                user.getRewardPoint(),
                user.getCompletedOrderCount(),
                user.getJoinedDate(),
                user.isAlarmConsent(),
                addresses.stream()
                        .map(address -> AdminMemberAddressResponse.from(address, decrypt))
                        .toList(),
                joinedAt,
                joinedAt
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
