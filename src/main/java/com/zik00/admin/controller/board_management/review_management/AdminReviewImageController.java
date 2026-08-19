package com.zik00.admin.controller.board_management.review_management;

import com.zik00.admin.dto.board_management.review_management.AdminReviewImageUploadResponse;
import com.zik00.admin.service.Web_management.HomepageImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/board-management/review-images")
public class AdminReviewImageController {
    private final HomepageImageStorageService imageStorageService;

    public AdminReviewImageController(HomepageImageStorageService imageStorageService) {
        this.imageStorageService = imageStorageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AdminReviewImageUploadResponse upload(@RequestParam("image") MultipartFile image) {
        return new AdminReviewImageUploadResponse(imageStorageService.store(image));
    }
}
