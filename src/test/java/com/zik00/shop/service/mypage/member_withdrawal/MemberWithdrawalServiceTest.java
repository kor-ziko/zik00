package com.zik00.shop.service.mypage.member_withdrawal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.shop.domain.User;
import com.zik00.shop.dto.mypage.member_withdrawal.MemberWithdrawalRequest;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MemberWithdrawalServiceTest {
    private final AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
    private final RedisRefreshTokenStore refreshTokenStore = mock(RedisRefreshTokenStore.class);
    private final MemberWithdrawalService service = new MemberWithdrawalService(
            authenticatedUserService,
            refreshTokenStore
    );

    @Test
    void withdrawChangesStatusAndRevokesEverySession() {
        User user = activeUser();
        when(authenticatedUserService.getCurrentUser()).thenReturn(user);

        service.withdraw(new MemberWithdrawalRequest("회원탈퇴"));

        assertThat(user.getMemberStatus()).isEqualTo("WITHDRAWN");
        assertThat(user.getWithdrawnAt()).isNotNull();
        verify(refreshTokenStore).revokeAllForUser(user.getAccessId());
    }

    @Test
    void withdrawRejectsIncorrectConfirmationWithoutChangingMember() {
        User user = activeUser();

        assertThatThrownBy(() -> service.withdraw(new MemberWithdrawalRequest("탈퇴")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("회원탈퇴");

        assertThat(user.getMemberStatus()).isEqualTo("ACTIVE");
        verify(authenticatedUserService, never()).getCurrentUser();
        verify(refreshTokenStore, never()).revokeAllForUser(user.getAccessId());
    }

    private User activeUser() {
        return new User(
                "회원", null, "", "닉네임", "", "login", 0, 0,
                "", "member@example.com", 0, LocalDate.now(), "일반회원", false
        );
    }
}
