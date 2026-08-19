package com.zik00.shop.service.review;

import com.zik00.shop.domain.review.ServiceReview;
import com.zik00.shop.dto.review.RatingCountResponse;
import com.zik00.shop.dto.review.ReviewItemResponse;
import com.zik00.shop.dto.review.ReviewListResponse;
import com.zik00.shop.repository.review.ServiceReviewRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.stream.IntStream;

@Service
public class ReviewService {
    private static final int MAX_PAGE_SIZE = 18;
    private final ServiceReviewRepository repository;

    public ReviewService(ServiceReviewRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public ReviewListResponse findReviews(Integer rating, String sort, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        Sort reviewSort = "rating".equalsIgnoreCase(sort)
                ? Sort.by(Sort.Order.desc("rating"), Sort.Order.desc("createdAt"))
                : Sort.by(Sort.Order.desc("createdAt"));
        PageRequest pageable = PageRequest.of(safePage, safeSize, reviewSort);
        Page<ServiceReview> reviews = rating != null && rating >= 1 && rating <= 5
                ? repository.findByPublishedTrueAndRating(rating, pageable)
                : repository.findByPublishedTrue(pageable);

        var countByRating = repository.findPublishedRatingCounts().stream()
                .collect(java.util.stream.Collectors.toMap(
                        item -> item.getRating(),
                        item -> item.getCount()
                ));

        return new ReviewListResponse(
                reviews.getContent().stream().map(ReviewItemResponse::from).toList(),
                Math.round(repository.findPublishedAverageRating() * 10.0) / 10.0,
                repository.countByPublishedTrue(),
                IntStream.iterate(5, value -> value >= 1, value -> value - 1)
                        .mapToObj(value -> new RatingCountResponse(value, countByRating.getOrDefault(value, 0L)))
                        .toList(),
                reviews.getNumber(),
                reviews.getSize(),
                reviews.getTotalPages()
        );
    }
}
