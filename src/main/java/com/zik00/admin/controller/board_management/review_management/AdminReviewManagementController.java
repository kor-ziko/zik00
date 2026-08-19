package com.zik00.admin.controller.board_management.review_management;

import com.zik00.admin.dto.AdminSession;
import com.zik00.admin.dto.board_management.review_management.AdminReviewCommentCreateRequest;
import com.zik00.admin.dto.board_management.review_management.AdminReviewResponse;
import com.zik00.admin.dto.board_management.review_management.AdminReviewUpdateRequest;
import com.zik00.admin.service.AdminAuthService;
import com.zik00.admin.service.board_management.review_management.AdminReviewManagementService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/board-management/reviews")
public class AdminReviewManagementController {
    private final AdminReviewManagementService service;
    private final AdminAuthService adminAuthService;

    public AdminReviewManagementController(
            AdminReviewManagementService service,
            AdminAuthService adminAuthService
    ) {
        this.service = service;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public List<AdminReviewResponse> findAll() {
        return service.findAll();
    }

    @PutMapping("/{reviewId}")
    public AdminReviewResponse update(
            @PathVariable long reviewId,
            @Valid @RequestBody AdminReviewUpdateRequest request
    ) {
        return service.update(reviewId, request);
    }

    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long reviewId) {
        service.delete(reviewId);
    }

    @PostMapping("/{reviewId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public AdminReviewResponse addComment(
            @PathVariable long reviewId,
            @Valid @RequestBody AdminReviewCommentCreateRequest request,
            Authentication authentication
    ) {
        AdminSession adminSession = adminAuthService.current(authentication);
        return service.addComment(reviewId, request, adminSession);
    }
}
