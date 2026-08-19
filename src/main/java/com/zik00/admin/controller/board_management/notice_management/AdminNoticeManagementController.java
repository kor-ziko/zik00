package com.zik00.admin.controller.board_management.notice_management;

import com.zik00.admin.dto.board_management.notice_management.AdminNoticeResponse;
import com.zik00.admin.dto.board_management.notice_management.AdminNoticeUpdateRequest;
import com.zik00.admin.service.board_management.notice_management.AdminNoticeManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/board-management/notices")
public class AdminNoticeManagementController {
    private final AdminNoticeManagementService service;

    public AdminNoticeManagementController(AdminNoticeManagementService service) {
        this.service = service;
    }

    @GetMapping
    public List<AdminNoticeResponse> findAll() {
        return service.findAll();
    }

    @GetMapping("/{noticeId}")
    public AdminNoticeResponse findOne(@PathVariable long noticeId) {
        return service.findOne(noticeId);
    }

    @PutMapping("/{noticeId}")
    public AdminNoticeResponse update(
            @PathVariable long noticeId,
            @Valid @RequestBody AdminNoticeUpdateRequest request
    ) {
        return service.update(noticeId, request);
    }

    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long noticeId) {
        service.delete(noticeId);
    }
}
