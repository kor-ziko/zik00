package com.zik00.shop.repository;

import java.util.Collection;
import java.util.List;

import com.zik00.shop.domain.InquiryComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryCommentRepository extends JpaRepository<InquiryComment, Long> {
    // @Suil - 관리자 문의 목록에 댓글 개수를 표시
    long countByInquiryId(long inquiryId);

    @Query("""
            select c
            from InquiryComment c
            where c.inquiryId in ?1
            order by c.commentId asc
            """)
    List<InquiryComment> findByInquiryIds(Collection<Long> inquiryIds);
}
