package com.zik00.shop.repository.notice;

import com.zik00.shop.domain.notice.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    Page<Notice> findByPublishedTrueOrderByPinnedDescPublishedAtDesc(Pageable pageable);

    Page<Notice> findByPublishedTrueAndCategoryOrderByPinnedDescPublishedAtDesc(
            String category,
            Pageable pageable
    );

    Optional<Notice> findByIdAndPublishedTrue(Long id);

    @Query("select distinct n.category from Notice n where n.published = true order by n.category")
    java.util.List<String> findPublishedCategories();
}
