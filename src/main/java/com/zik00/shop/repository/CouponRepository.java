package com.zik00.shop.repository;

import java.util.List;

import com.zik00.shop.domain.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface CouponRepository extends JpaRepository<Coupon, Long> {
    @Query("""
            select c
            from Coupon c
            where c.memberId = ?1
            order by c.expiredDate asc, c.couponId asc
            """)
    List<Coupon> findUserCoupons(long memberId);

    @Query("""
            select count(c)
            from Coupon c
            where c.memberId = ?1
            """)
    long countUserCoupons(long memberId);
}
