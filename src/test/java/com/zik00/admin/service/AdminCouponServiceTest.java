package com.zik00.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.admin.dto.AdminMemberCouponIssueRequest;
import com.zik00.admin.repository.AdminCouponRepository;
import com.zik00.admin.repository.CouponTemplateRepository;
import com.zik00.shop.domain.Coupon;
import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class AdminCouponServiceTest {
    @Test
    void memberCouponsUseOneBulkMemberLookupAndOneBulkSave() {
        CouponTemplateRepository templateRepository = mock(CouponTemplateRepository.class);
        AdminCouponRepository couponRepository = mock(AdminCouponRepository.class);
        UserRepository userRepository = mock(UserRepository.class);
        AdminCouponService service = new AdminCouponService(templateRepository, couponRepository, userRepository);
        User firstUser = user(1L, "first");
        User secondUser = user(2L, "second");
        List<Long> memberIds = List.of(1L, 2L);
        AdminMemberCouponIssueRequest request = new AdminMemberCouponIssueRequest(
                null, memberIds, "welcome", "fixed", 1_000, 0, null, null
        );
        when(userRepository.findAllById(memberIds)).thenReturn(List.of(firstUser, secondUser));
        when(couponRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        var issuedCoupons = service.issueMemberCoupon(request);

        assertThat(issuedCoupons).extracting(coupon -> coupon.memberId()).containsExactly(1L, 2L);
        verify(userRepository).findAllById(memberIds);
        verify(userRepository, never()).findById(1L);
        verify(userRepository, never()).findById(2L);
        verify(couponRepository).saveAll(anyList());
        verify(couponRepository, never()).save(org.mockito.ArgumentMatchers.any(Coupon.class));
    }

    private User user(long memberId, String loginId) {
        User user = new User(
                "name", null, "", loginId, "", loginId, 0, 0,
                "", loginId + "@example.com", 0, LocalDate.now(), "member", false
        );
        ReflectionTestUtils.setField(user, "memberId", memberId);
        return user;
    }
}
