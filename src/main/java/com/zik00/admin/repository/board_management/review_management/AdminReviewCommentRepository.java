package com.zik00.admin.repository.board_management.review_management;

import com.zik00.admin.domain.board_management.review_management.AdminReviewComment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface AdminReviewCommentRepository extends JpaRepository<AdminReviewComment, Long> {
    List<AdminReviewComment> findByReviewIdInOrderByCreatedAtAscIdAsc(Collection<Long> reviewIds);

    void deleteByReviewId(long reviewId);
}
