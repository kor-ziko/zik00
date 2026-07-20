package com.zik00.shop.service;

public class ExternalApiRateLimitException extends RuntimeException {
    public ExternalApiRateLimitException(String message) {
        super(message);
    }
}
