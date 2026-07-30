package com.zik00.shop.controller;

import java.time.Duration;

import com.zik00.shop.service.ProductImageProxyService;
import com.zik00.shop.service.ProductImageProxyService.ProxyImage;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/product-images")
public class ProductImageProxyController {
    private final ProductImageProxyService productImageProxyService;

    public ProductImageProxyController(ProductImageProxyService productImageProxyService) {
        this.productImageProxyService = productImageProxyService;
    }

    @GetMapping("/proxy")
    public ResponseEntity<byte[]> proxy(@RequestParam String url) {
        ProxyImage image = productImageProxyService.fetch(url);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.maxAge(Duration.ofDays(7)).cachePublic())
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline")
                .header(HttpHeaders.VARY, HttpHeaders.ACCEPT)
                .header("Cross-Origin-Resource-Policy", "cross-origin")
                .contentType(image.contentType())
                .contentLength(image.body().length)
                .body(image.body());
    }
}
