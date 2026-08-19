package com.zik00.admin.service.board_management.notice_management;

import com.zik00.admin.dto.board_management.notice_management.AdminNoticeResponse;
import com.zik00.admin.dto.board_management.notice_management.AdminNoticeUpdateRequest;
import com.zik00.admin.repository.board_management.notice_management.AdminBoardNoticeRepository;
import com.zik00.shop.domain.notice.Notice;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminNoticeManagementService {
    private final AdminBoardNoticeRepository repository;

    public AdminNoticeManagementService(AdminBoardNoticeRepository repository) {
        this.repository = repository;
    }

    public List<AdminNoticeResponse> findAll() {
        return repository.findAllByOrderByPinnedDescPublishedAtDescIdDesc().stream()
                .map(AdminNoticeResponse::from)
                .toList();
    }

    public AdminNoticeResponse findOne(long noticeId) {
        return AdminNoticeResponse.from(findEntity(noticeId));
    }

    @Transactional
    public AdminNoticeResponse update(long noticeId, AdminNoticeUpdateRequest request) {
        Notice notice = findEntity(noticeId);
        notice.update(
                request.category().trim(), request.title().trim(), request.content().trim(),
                request.pinned(), request.published(),
                request.publishedAt() == null ? LocalDateTime.now() : request.publishedAt()
        );
        return AdminNoticeResponse.from(notice);
    }

    @Transactional
    public void delete(long noticeId) {
        Notice notice = findEntity(noticeId);
        repository.delete(notice);
    }

    private Notice findEntity(long noticeId) {
        return repository.findById(noticeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."));
    }
}
