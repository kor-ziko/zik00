package com.zik00.admin.repository.board_management.review_management;

import com.zik00.shop.domain.review.ServiceReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminBoardReviewRepository extends JpaRepository<ServiceReview, Long> {
    List<ServiceReview> findAllByOrderByCreatedAtDescIdDesc();
}
