package com.zik00.shop.controller.homepage;

import com.zik00.admin.service.Web_management.HomepageImageStorageService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/homepage-images")
public class HomepageImageController {
    private final HomepageImageStorageService storageService;

    public HomepageImageController(HomepageImageStorageService storageService) {
        this.storageService = storageService;
    }

    @GetMapping("/{fileName:.+}")
    public ResponseEntity<FileSystemResource> image(@PathVariable String fileName) {
        return storageService.load(fileName)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .contentLength(image.contentLength())
                        .cacheControl(CacheControl.maxAge(30, TimeUnit.DAYS).cachePublic())
                        .body(new FileSystemResource(image.path())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
