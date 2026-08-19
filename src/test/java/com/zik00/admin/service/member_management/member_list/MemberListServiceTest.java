package com.zik00.admin.service.member_management.member_list;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.admin.repository.member_management.member_list.MemberListRepository;
import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class MemberListServiceTest {
    @Test
    void withdrawChangesMemberStatusAndRevokesSessions() {
        MemberListRepository repository = mock(MemberListRepository.class);
        PiiEncryptionService encryptionService = mock(PiiEncryptionService.class);
        RedisRefreshTokenStore refreshTokenStore = mock(RedisRefreshTokenStore.class);
        MemberListService service = new MemberListService(repository, encryptionService, refreshTokenStore);
        User user = new User(
                "회원", null, "", "닉네임", "", "login", 0, 0,
                "", "member@example.com", 0, LocalDate.now(), "일반회원", false
        );
        when(repository.findById(1L)).thenReturn(Optional.of(user));

        service.withdraw(1L);

        assertThat(user.getMemberStatus()).isEqualTo("WITHDRAWN");
        assertThat(user.getWithdrawnAt()).isNotNull();
        verify(refreshTokenStore).revokeAllForUser(user.getAccessId());
    }
}
