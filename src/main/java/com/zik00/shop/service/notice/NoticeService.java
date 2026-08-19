package com.zik00.shop.service.notice;

import com.zik00.shop.domain.notice.Notice;
import com.zik00.shop.dto.notice.NoticeDetailResponse;
import com.zik00.shop.dto.notice.NoticeListResponse;
import com.zik00.shop.dto.notice.NoticeSummaryResponse;
import com.zik00.shop.repository.notice.NoticeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class NoticeService {
    private static final int MAX_PAGE_SIZE = 20;
    private final NoticeRepository repository;

    public NoticeService(NoticeRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public NoticeListResponse findNotices(String category, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        PageRequest pageable = PageRequest.of(safePage, safeSize);
        Page<Notice> notices = category == null || category.isBlank() || "전체".equals(category)
                ? repository.findByPublishedTrueOrderByPinnedDescPublishedAtDesc(pageable)
                : repository.findByPublishedTrueAndCategoryOrderByPinnedDescPublishedAtDesc(category.trim(), pageable);

        return new NoticeListResponse(
                notices.getContent().stream().map(NoticeSummaryResponse::from).toList(),
                repository.findPublishedCategories(),
                notices.getNumber(),
                notices.getSize(),
                notices.getTotalElements(),
                notices.getTotalPages()
        );
    }

    @Transactional(readOnly = true)
    public NoticeDetailResponse findNotice(long id) {
        return repository.findByIdAndPublishedTrue(id)
                .map(NoticeDetailResponse::from)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "공지사항을 찾을 수 없습니다."));
    }
}
