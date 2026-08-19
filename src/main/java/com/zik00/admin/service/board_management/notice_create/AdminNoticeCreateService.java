package com.zik00.admin.service.board_management.notice_create;

import com.zik00.admin.dto.board_management.notice_create.AdminNoticeCreateRequest;
import com.zik00.admin.dto.board_management.notice_management.AdminNoticeResponse;
import com.zik00.admin.repository.board_management.notice_management.AdminBoardNoticeRepository;
import com.zik00.shop.domain.notice.Notice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class AdminNoticeCreateService {
    private final AdminBoardNoticeRepository repository;

    public AdminNoticeCreateService(AdminBoardNoticeRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminNoticeResponse create(AdminNoticeCreateRequest request) {
        Notice notice = new Notice(
                request.category().trim(), request.title().trim(), request.content().trim(),
                request.pinned(), request.published(),
                request.publishedAt() == null ? LocalDateTime.now() : request.publishedAt()
        );
        return AdminNoticeResponse.from(repository.save(notice));
    }
}
