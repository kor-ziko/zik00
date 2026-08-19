package com.zik00.shop.service.mypage.member_withdrawal;

import com.zik00.shop.domain.User;
import com.zik00.shop.dto.mypage.member_withdrawal.MemberWithdrawalRequest;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MemberWithdrawalService {
    private static final String CONFIRMATION_TEXT = "회원탈퇴";

    private final AuthenticatedUserService authenticatedUserService;
    private final RedisRefreshTokenStore refreshTokenStore;

    public MemberWithdrawalService(
            AuthenticatedUserService authenticatedUserService,
            RedisRefreshTokenStore refreshTokenStore
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.refreshTokenStore = refreshTokenStore;
    }

    @Transactional
    public void withdraw(MemberWithdrawalRequest request) {
        if (!CONFIRMATION_TEXT.equals(request.confirmation().trim())) {
            throw new IllegalArgumentException("확인 문구에 회원탈퇴를 정확히 입력해주세요.");
        }

        User user = authenticatedUserService.getCurrentUser();
        if ("WITHDRAWN".equals(user.getMemberStatus())) {
            throw new IllegalStateException("이미 탈퇴 처리된 회원입니다.");
        }

        user.changeMemberStatus("WITHDRAWN");
        refreshTokenStore.revokeAllForUser(user.getAccessId());
    }
}
