package com.zik00.shop.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class WebClientOrigins {
    private final String clientBaseUrl;
    private final List<String> allowedOrigins;

    public WebClientOrigins(
            @Value("${shop.frontend.base-url:http://localhost:5174}") String clientBaseUrl,
            @Value("${shop.admin.base-url:http://127.0.0.1:5173}") String adminBaseUrl
    ) {
        this.clientBaseUrl = normalize(clientBaseUrl);
        this.allowedOrigins = List.of(this.clientBaseUrl, normalize(adminBaseUrl)).stream()
                .distinct()
                .toList();
    }

    public String clientBaseUrl() {
        return clientBaseUrl;
    }

    public List<String> allowedOrigins() {
        return allowedOrigins;
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Web client origin must not be blank.");
        }

        String normalized = value.strip();
        int end = normalized.length();
        while (end > 0 && normalized.charAt(end - 1) == '/') {
            end--;
        }
        return normalized.substring(0, end);
    }
}
