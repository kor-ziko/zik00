package com.zik00.admin.repository.board_management.notice_management;

import com.zik00.admin.domain.board_management.notice_management.NoticeCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeCategoryRepository extends JpaRepository<NoticeCategory, Long> {
    List<NoticeCategory> findAllByOrderByDisplayOrderAscIdAsc();
    boolean existsByNameIgnoreCase(String name);
}
