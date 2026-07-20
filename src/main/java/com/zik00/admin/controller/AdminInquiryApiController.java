package com.zik00.admin.controller;

import java.util.List;

import com.zik00.admin.dto.AdminInquiryDetailResponse;
import com.zik00.admin.dto.AdminInquirySummaryResponse;
import com.zik00.admin.dto.AdminSession;
import com.zik00.admin.service.AdminAuthService;
import com.zik00.admin.service.AdminInquiryService;
import org.springframework.security.core.Authentication;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
// @dev - 관리자 문의 처리 API
@RestController
@RequestMapping("/api/admin/inquiries")
public class AdminInquiryApiController {
    private final AdminInquiryService adminInquiryService;
    private final AdminAuthService adminAuthService;

    public AdminInquiryApiController(
            AdminInquiryService adminInquiryService,
            AdminAuthService adminAuthService
    ) {
        this.adminInquiryService = adminInquiryService;
        this.adminAuthService = adminAuthService;
    }

    @GetMapping
    public List<AdminInquirySummaryResponse> findInquiries() {
        return adminInquiryService.findInquiries();
    }

    @GetMapping("/{inquiryId}")
    public AdminInquiryDetailResponse findInquiry(@PathVariable long inquiryId) {
        return adminInquiryService.findInquiry(inquiryId);
    }

    @PostMapping(value = "/{inquiryId}/replies", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AdminInquiryDetailResponse reply(
            @PathVariable long inquiryId,
            @RequestParam String content,
            @RequestParam(required = false) List<MultipartFile> images,
            Authentication authentication
    ) {
        AdminSession adminSession = adminAuthService.current(authentication);
        return adminInquiryService.reply(inquiryId, content, images, adminSession);
    }

    @GetMapping("/images/{imageUuid}")
    public ResponseEntity<Resource> inquiryImage(@PathVariable String imageUuid) {
        return adminInquiryService.getImageDownload(imageUuid)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .contentLength(image.contentLength())
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                                .filename(image.fileName())
                                .build()
                                .toString())
                        .header("X-Content-Type-Options", "nosniff")
                        .body((Resource) new FileSystemResource(image.imagePath())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
