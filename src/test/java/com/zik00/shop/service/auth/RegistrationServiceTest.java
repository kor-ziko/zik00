package com.zik00.shop.service.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.User;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.UserRepository;
import com.zik00.shop.service.JapanPostalCodeSearchService;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class RegistrationServiceTest {
    @Test
    void withdrawnOAuthMemberCanRegisterAgainWithUpdatedProfileAndAddress() {
        UserRepository userRepository = mock(UserRepository.class);
        DeliveryAddressRepository addressRepository = mock(DeliveryAddressRepository.class);
        RegistrationService service = new RegistrationService(
                mock(AuthenticatedUserService.class),
                userRepository,
                addressRepository,
                mock(JapanPostalCodeSearchService.class)
        );
        User user = new User(
                "기존 이름", LocalDate.of(1990, 1, 1), "M", "기존 닉네임", "03-1111-1111",
                "google:subject", 0, 0, "090-1111-1111", "member@example.com", 0,
                LocalDate.now(), "일반회원", false
        );
        user.completeRegistration(
                "기존 이름", "キゾン", LocalDate.of(1990, 1, 1), "M", "기존 닉네임",
                "03-1111-1111", "090-1111-1111", false
        );
        ReflectionTestUtils.setField(user, "memberId", 1L);
        user.changeMemberStatus("WITHDRAWN");
        DeliveryAddress address = new DeliveryAddress(
                1, 0, "기존 배송지", "기존 이름", "090-1111-1111",
                "1000001", "東京都", "기존 주소", true
        );
        when(userRepository.findByLoginId("google:subject")).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);
        when(addressRepository.findUserAddresses(user.getMemberId())).thenReturn(List.of(address));
        var accepted = new PendingOAuthRegistrationService.AcceptedOAuthRegistration(
                new OAuthProfile("google", "subject", "member@example.com", "회원"),
                true
        );
        var detail = new RegistrationService.PreparedRegistration(
                "새 이름", "アタラシイ", LocalDate.of(1995, 5, 5), "F", "새 닉네임",
                "03-2222-2222", "090-2222-2222", "1500001", "東京都", "새 주소"
        );

        User result = service.completeOAuthRegistration(accepted, detail);

        assertThat(result.getMemberStatus()).isEqualTo("ACTIVE");
        assertThat(result.getWithdrawnAt()).isNull();
        assertThat(result.getName()).isEqualTo("새 이름");
        assertThat(result.getNickname()).isEqualTo("새 닉네임");
        assertThat(address.getAddressName()).isEqualTo("기본 배송지");
        assertThat(address.getZipCode()).isEqualTo("1500001");
        assertThat(address.getDetailAddress()).isEqualTo("새 주소");
        assertThat(address.isDefaultAddress()).isTrue();
        verify(userRepository).save(user);
    }
}
