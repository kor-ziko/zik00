package com.zik00.admin.controller.board_management.notice_create;

import com.zik00.admin.dto.board_management.notice_create.AdminNoticeCreateRequest;
import com.zik00.admin.dto.board_management.notice_management.AdminNoticeResponse;
import com.zik00.admin.service.board_management.notice_create.AdminNoticeCreateService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/board-management/notices")
public class AdminNoticeCreateController {
    private final AdminNoticeCreateService service;

    public AdminNoticeCreateController(AdminNoticeCreateService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminNoticeResponse create(@Valid @RequestBody AdminNoticeCreateRequest request) {
        return service.create(request);
    }
}
