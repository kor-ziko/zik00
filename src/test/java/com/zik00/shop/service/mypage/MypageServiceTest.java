package com.zik00.shop.service.mypage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.shop.domain.Coupon;
import com.zik00.shop.domain.Purchase;
import com.zik00.shop.domain.User;
import com.zik00.shop.repository.CouponRepository;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.InquiryCommentImageRepository;
import com.zik00.shop.repository.InquiryCommentRepository;
import com.zik00.shop.repository.InquiryImageRepository;
import com.zik00.shop.repository.InquiryRepository;
import com.zik00.shop.repository.PurchaseRepository;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class MypageServiceTest {
    @Test
    void dashboardReusesLoadedOrdersAndCouponsForSummaryCounts() {
        AuthenticatedUserService authenticatedUserService = mock(AuthenticatedUserService.class);
        DeliveryAddressRepository deliveryAddressRepository = mock(DeliveryAddressRepository.class);
        CouponRepository couponRepository = mock(CouponRepository.class);
        PurchaseRepository purchaseRepository = mock(PurchaseRepository.class);
        InquiryRepository inquiryRepository = mock(InquiryRepository.class);
        InquiryCommentRepository inquiryCommentRepository = mock(InquiryCommentRepository.class);
        InquiryCommentImageRepository inquiryCommentImageRepository = mock(InquiryCommentImageRepository.class);
        InquiryImageRepository inquiryImageRepository = mock(InquiryImageRepository.class);
        InquiryImageStorageService inquiryImageStorageService = mock(InquiryImageStorageService.class);
        MypageService service = new MypageService(
                authenticatedUserService,
                deliveryAddressRepository,
                couponRepository,
                purchaseRepository,
                inquiryRepository,
                inquiryCommentRepository,
                inquiryCommentImageRepository,
                inquiryImageRepository,
                inquiryImageStorageService
        );
        User user = new User(
                "name", null, "", "nickname", "", "login", 10, 20,
                "", "email@example.com", 0, LocalDate.now(), "member", false
        );
        List<Purchase> purchases = List.of(
                purchaseWithStatus("주문완료"),
                purchaseWithStatus("배송중"),
                purchaseWithStatus("배송완료"),
                purchaseWithStatus(null)
        );
        List<Coupon> coupons = List.of(
                coupon(1),
                coupon(2)
        );
        when(authenticatedUserService.getCurrentUser()).thenReturn(user);
        when(purchaseRepository.findUserPurchases(0L)).thenReturn(purchases);
        when(couponRepository.findUserCoupons(0L)).thenReturn(coupons);
        when(inquiryRepository.countUserInquiries(0L)).thenReturn(3L);

        MypageService.DashboardData dashboard = service.getDashboard();

        assertThat(dashboard.summary().getCompletedOrderCount()).isEqualTo(1);
        assertThat(dashboard.summary().getDeliveryTrackingCount()).isEqualTo(2);
        assertThat(dashboard.summary().getInquiryCount()).isEqualTo(3);
        assertThat(dashboard.summary().getCouponCount()).isEqualTo(2);
        assertThat(dashboard.recentOrders()).hasSize(4);
        assertThat(dashboard.coupons()).hasSize(2);
        verify(purchaseRepository, never()).countUserOrdersByStatus(0L, "주문완료");
        verify(purchaseRepository, never()).countUserOrdersByStatus(0L, "배송");
        verify(couponRepository, never()).countUserCoupons(0L);
    }

    private Purchase purchaseWithStatus(String status) {
        return new Purchase(0, 0, "order", "product", 1, 1_000, status, LocalDate.now());
    }

    private Coupon coupon(long id) {
        return new Coupon(id, 0, "coupon", "fixed", 100, 0, null, null, false);
    }
}
