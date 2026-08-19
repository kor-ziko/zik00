package com.zik00.admin.service.board_management.review_management;

import com.zik00.admin.domain.board_management.review_management.AdminReviewComment;
import com.zik00.admin.dto.AdminSession;
import com.zik00.admin.dto.board_management.review_management.AdminReviewCommentCreateRequest;
import com.zik00.admin.dto.board_management.review_management.AdminReviewCommentResponse;
import com.zik00.admin.dto.board_management.review_management.AdminReviewResponse;
import com.zik00.admin.dto.board_management.review_management.AdminReviewUpdateRequest;
import com.zik00.admin.repository.board_management.review_management.AdminBoardReviewRepository;
import com.zik00.admin.repository.board_management.review_management.AdminReviewCommentRepository;
import com.zik00.shop.domain.review.ServiceReview;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class AdminReviewManagementService {
    private final AdminBoardReviewRepository reviewRepository;
    private final AdminReviewCommentRepository commentRepository;

    public AdminReviewManagementService(
            AdminBoardReviewRepository reviewRepository,
            AdminReviewCommentRepository commentRepository
    ) {
        this.reviewRepository = reviewRepository;
        this.commentRepository = commentRepository;
    }

    public List<AdminReviewResponse> findAll() {
        List<ServiceReview> reviews = reviewRepository.findAllByOrderByCreatedAtDescIdDesc();
        if (reviews.isEmpty()) return List.of();
        Map<Long, List<AdminReviewCommentResponse>> comments = commentRepository
                .findByReviewIdInOrderByCreatedAtAscIdAsc(reviews.stream().map(ServiceReview::getId).toList())
                .stream()
                .collect(Collectors.groupingBy(
                        AdminReviewComment::getReviewId,
                        Collectors.mapping(AdminReviewCommentResponse::from, Collectors.toList())
                ));
        return reviews.stream()
                .map(review -> AdminReviewResponse.from(review, comments.getOrDefault(review.getId(), List.of())))
                .toList();
    }

    @Transactional
    public AdminReviewResponse update(long reviewId, AdminReviewUpdateRequest request) {
        ServiceReview review = findReview(reviewId);
        review.update(
                request.authorName().trim(), request.title().trim(), request.content().trim(),
                request.rating(), request.productName().trim(), normalizeNullable(request.imageUrl()),
                request.featured(), request.published()
        );
        return response(review);
    }

    @Transactional
    public void delete(long reviewId) {
        ServiceReview review = findReview(reviewId);
        commentRepository.deleteByReviewId(reviewId);
        reviewRepository.delete(review);
    }

    @Transactional
    public AdminReviewResponse addComment(
            long reviewId,
            AdminReviewCommentCreateRequest request,
            AdminSession adminSession
    ) {
        ServiceReview review = findReview(reviewId);
        commentRepository.save(new AdminReviewComment(
                reviewId, adminSession.adminId(), adminSession.name(), request.content().trim()
        ));
        return response(review);
    }

    private AdminReviewResponse response(ServiceReview review) {
        List<AdminReviewCommentResponse> comments = commentRepository
                .findByReviewIdInOrderByCreatedAtAscIdAsc(List.of(review.getId()))
                .stream().map(AdminReviewCommentResponse::from).toList();
        return AdminReviewResponse.from(review, comments);
    }

    private ServiceReview findReview(long reviewId) {
        return reviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "리뷰를 찾을 수 없습니다."));
    }

    private String normalizeNullable(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }
}
