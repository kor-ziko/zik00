package com.zik00.admin.repository;

import com.zik00.shop.domain.Coupon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AdminCouponRepository extends JpaRepository<Coupon, Long> {
    @Query("""
            select c
            from Coupon c
            order by c.couponId desc
            """)
    List<Coupon> findIssuedCoupons();
}
