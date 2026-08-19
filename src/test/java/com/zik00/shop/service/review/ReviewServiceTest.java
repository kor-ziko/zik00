package com.zik00.shop.service.review;

import com.zik00.shop.domain.review.ServiceReview;
import com.zik00.shop.repository.review.RatingCountProjection;
import com.zik00.shop.repository.review.ServiceReviewRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReviewServiceTest {
    @Test
    void returnsPublishedReviewsAndRatingSummary() {
        ServiceReview review = mock(ServiceReview.class);
        when(review.getId()).thenReturn(1L);
        when(review.getAuthorName()).thenReturn("테스터***");
        when(review.getTitle()).thenReturn("만족한 주문");
        when(review.getContent()).thenReturn("배송 상태를 확인하기 편했습니다.");
        when(review.getRating()).thenReturn(5);
        when(review.getProductName()).thenReturn("테스트 상품");
        when(review.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 8, 10, 12, 0));

        RatingCountProjection count = mock(RatingCountProjection.class);
        when(count.getRating()).thenReturn(5);
        when(count.getCount()).thenReturn(1L);

        ServiceReviewRepository repository = mock(ServiceReviewRepository.class);
        when(repository.findByPublishedTrue(any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(review), PageRequest.of(0, 9), 1));
        when(repository.findPublishedRatingCounts()).thenReturn(List.of(count));
        when(repository.findPublishedAverageRating()).thenReturn(5.0);
        when(repository.countByPublishedTrue()).thenReturn(1L);

        var response = new ReviewService(repository).findReviews(null, "latest", 0, 9);

        assertThat(response.items()).extracting("title").containsExactly("만족한 주문");
        assertThat(response.averageRating()).isEqualTo(5.0);
        assertThat(response.ratingCounts()).extracting("count").containsExactly(1L, 0L, 0L, 0L, 0L);
    }
}
