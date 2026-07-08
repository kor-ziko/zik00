package com.zik00.shop.repository;

import java.util.List;

import com.zik00.shop.domain.InquiryImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface InquiryImageRepository extends JpaRepository<InquiryImage, Long> {
    @Query("""
            select i
            from InquiryImage i
            where i.inquiryId in ?1
            order by i.imageId asc
            """)
    List<InquiryImage> findByInquiryIds(List<Long> inquiryIds);
}
