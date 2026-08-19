package com.zik00.admin.controller.board_management.notice_create;

import com.zik00.admin.dto.board_management.notice_create.AdminNoticeCategoryCreateRequest;
import com.zik00.admin.dto.board_management.notice_management.AdminNoticeCategoryResponse;
import com.zik00.admin.service.board_management.notice_create.AdminNoticeCategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/board-management/notice-categories")
public class AdminNoticeCategoryController {
    private final AdminNoticeCategoryService service;

    public AdminNoticeCategoryController(AdminNoticeCategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminNoticeCategoryResponse> findAll() {
        return service.findAll();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminNoticeCategoryResponse create(@Valid @RequestBody AdminNoticeCategoryCreateRequest request) {
        return service.create(request);
    }
}
