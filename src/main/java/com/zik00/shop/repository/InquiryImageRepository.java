package com.zik00.shop.repository;

import java.util.List;
import java.util.Optional;

import com.zik00.shop.domain.InquiryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryImageRepository extends JpaRepository<InquiryImage, Long> {
    // @Suil - 관리자 문의 목록에 사용자 첨부사진 개수를 표시
    long countByInquiryId(long inquiryId);

    @Query("""
            select i
            from InquiryImage i
            where i.inquiryId in ?1
            order by i.imageId asc
            """)
    List<InquiryImage> findByInquiryIds(List<Long> inquiryIds);

    @Query("""
            select image
            from InquiryImage image, Inquiry inquiry
            where image.imageUuid = ?1
              and image.inquiryId = inquiry.inquiryId
              and inquiry.memberId = ?2
            """)
    Optional<InquiryImage> findUserImageByUuid(String imageUuid, long memberId);

    // @Suil - 관리자 화면에서 사용자 문의 사진을 조회
    Optional<InquiryImage> findByImageUuid(String imageUuid);
}
