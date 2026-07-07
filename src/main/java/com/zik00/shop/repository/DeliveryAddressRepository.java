package com.zik00.shop.repository;

import java.util.List;
import java.util.Optional;

import com.zik00.shop.domain.DeliveryAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface DeliveryAddressRepository extends JpaRepository<DeliveryAddress, Long> {
    @Query("""
            select a
            from DeliveryAddress a
            where a.memberId = ?1
            order by a.defaultAddress desc, a.id asc
            """)
    List<DeliveryAddress> findUserAddresses(long memberId);

    @Query("""
            select a
            from DeliveryAddress a
            where a.id = ?1
              and a.memberId = ?2
            """)
    Optional<DeliveryAddress> findUserAddress(long addressId, long memberId);
}
