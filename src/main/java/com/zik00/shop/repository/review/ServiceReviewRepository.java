package com.zik00.shop.repository.review;

import com.zik00.shop.domain.review.ServiceReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ServiceReviewRepository extends JpaRepository<ServiceReview, Long> {
    Page<ServiceReview> findByPublishedTrue(Pageable pageable);

    Page<ServiceReview> findByPublishedTrueAndRating(int rating, Pageable pageable);

    long countByPublishedTrue();

    @Query("select coalesce(avg(r.rating), 0) from ServiceReview r where r.published = true")
    double findPublishedAverageRating();

    @Query("select r.rating as rating, count(r) as count from ServiceReview r where r.published = true group by r.rating order by r.rating desc")
    List<RatingCountProjection> findPublishedRatingCounts();
}
