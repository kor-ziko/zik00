package com.zik00.shop.dto.mypage.member_withdrawal;

import jakarta.validation.constraints.NotBlank;

public record MemberWithdrawalRequest(
        @NotBlank(message = "회원탈퇴 확인 문구를 입력해주세요.")
        String confirmation
) {
}
