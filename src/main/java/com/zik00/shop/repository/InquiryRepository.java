package com.zik00.shop.repository;

import java.util.List;
import java.util.Optional;

import com.zik00.shop.domain.Inquiry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryRepository extends JpaRepository<Inquiry, Long> {
    // @Suil - 관리자 문의 목록을 최신순으로 조회
    List<Inquiry> findAllByOrderByInquiryIdDesc();

    // @Suil - 사용자 추가 댓글 등록 시 본인 문의를 조회
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

    @Query("""
            select i
            from Inquiry i
            where i.inquiryId = ?1
              and i.memberId = ?2
            """)
    Optional<Inquiry> findUserInquiry(long inquiryId, long memberId);
}
