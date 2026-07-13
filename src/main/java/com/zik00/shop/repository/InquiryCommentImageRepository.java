package com.zik00.shop.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import com.zik00.shop.domain.InquiryCommentImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// @Suil - 댓글별 답변 사진과 사용자 소유 문의 사진을 조회
public interface InquiryCommentImageRepository extends JpaRepository<InquiryCommentImage, Long> {
    @Query("""
            select image
            from InquiryCommentImage image
            where image.commentId in ?1
            order by image.commentImageId asc
            """)
    List<InquiryCommentImage> findByCommentIds(Collection<Long> commentIds);

    Optional<InquiryCommentImage> findByImageUuid(String imageUuid);

    @Query("""
            select image
            from InquiryCommentImage image, InquiryComment comment, Inquiry inquiry
            where image.imageUuid = ?1
              and image.commentId = comment.commentId
              and comment.inquiryId = inquiry.inquiryId
              and inquiry.memberId = ?2
            """)
    Optional<InquiryCommentImage> findUserImageByUuid(String imageUuid, long memberId);
}
