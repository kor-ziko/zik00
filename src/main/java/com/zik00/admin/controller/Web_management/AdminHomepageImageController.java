package com.zik00.admin.controller.Web_management;

import com.zik00.admin.dto.Web_management.HomepageImageUploadResponse;
import com.zik00.admin.service.Web_management.HomepageImageStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/admin/web-management/images")
public class AdminHomepageImageController {
    private final HomepageImageStorageService storageService;

    public AdminHomepageImageController(HomepageImageStorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public HomepageImageUploadResponse upload(@RequestParam("image") MultipartFile image) {
        return new HomepageImageUploadResponse(storageService.store(image));
    }
}
