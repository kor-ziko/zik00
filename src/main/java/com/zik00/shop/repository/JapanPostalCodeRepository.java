package com.zik00.shop.repository;

import java.util.List;

import com.zik00.shop.domain.JapanPostalCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JapanPostalCodeRepository extends JpaRepository<JapanPostalCode, Long> {
    @Query("""
            select p
            from JapanPostalCode p
            where p.postalCode = ?1
            order by p.prefecture asc, p.city asc, p.town asc
            """)
    List<JapanPostalCode> findByCode(String postalCode);
}
