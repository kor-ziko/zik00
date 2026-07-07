package com.zik00.shop.repository;

import java.util.List;

import com.zik00.shop.domain.Purchase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    @Query("""
            select p
            from Purchase p
            where p.memberId = ?1
            order by p.orderedDate desc, p.purchaseId desc
            """)
    List<Purchase> findUserPurchases(long memberId);

    @Query("""
            select count(p)
            from Purchase p
            where p.memberId = ?1
              and p.orderStatus like concat('%', ?2, '%')
            """)
    long countUserOrdersByStatus(long memberId, String orderStatus);
}
