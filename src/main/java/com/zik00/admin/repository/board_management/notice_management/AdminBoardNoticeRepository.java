package com.zik00.admin.repository.board_management.notice_management;

import com.zik00.shop.domain.notice.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AdminBoardNoticeRepository extends JpaRepository<Notice, Long> {
    List<Notice> findAllByOrderByPinnedDescPublishedAtDescIdDesc();
}
