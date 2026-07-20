package com.zik00.shop.controller;

import java.util.Map;

import com.zik00.shop.service.ExternalApiRateLimitException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ExternalApiRateLimitExceptionHandler {
    @ExceptionHandler(ExternalApiRateLimitException.class)
    public ResponseEntity<Map<String, String>> handleRateLimit(ExternalApiRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .header(HttpHeaders.RETRY_AFTER, "1")
                .body(Map.of("message", exception.getMessage()));
    }
}
