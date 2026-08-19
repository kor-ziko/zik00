package com.zik00.admin.service.board_management.notice_create;

import com.zik00.admin.domain.board_management.notice_management.NoticeCategory;
import com.zik00.admin.dto.board_management.notice_create.AdminNoticeCategoryCreateRequest;
import com.zik00.admin.dto.board_management.notice_management.AdminNoticeCategoryResponse;
import com.zik00.admin.repository.board_management.notice_management.NoticeCategoryRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class AdminNoticeCategoryService {
    private final NoticeCategoryRepository repository;

    public AdminNoticeCategoryService(NoticeCategoryRepository repository) {
        this.repository = repository;
    }

    public List<AdminNoticeCategoryResponse> findAll() {
        return repository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .map(AdminNoticeCategoryResponse::from)
                .toList();
    }

    @Transactional
    public AdminNoticeCategoryResponse create(AdminNoticeCategoryCreateRequest request) {
        String name = request.name().trim();
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "이미 등록된 분류입니다.");
        }
        int nextOrder = repository.findAllByOrderByDisplayOrderAscIdAsc().stream()
                .mapToInt(NoticeCategory::getDisplayOrder)
                .max()
                .orElse(0) + 1;
        return AdminNoticeCategoryResponse.from(repository.save(new NoticeCategory(name, nextOrder)));
    }
}
