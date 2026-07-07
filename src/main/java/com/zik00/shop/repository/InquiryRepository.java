package com.zik00.shop.repository;

import java.util.List;

import com.zik00.shop.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    @Query("""
            select i
            from Inquiry i
            where i.memberId = ?1
            order by i.inquiryId desc
            """)
    List<Inquiry> findUserInquiries(long memberId);

    @Query("""
            select count(i)
            from Inquiry i
            where i.memberId = ?1
            """)
    long countUserInquiries(long memberId);

    @Query("""
            select count(i) > 0
            from Inquiry i
            where i.inquiryId = ?1
              and i.memberId = ?2
            """)
    boolean existsUserInquiry(long inquiryId, long memberId);
}
