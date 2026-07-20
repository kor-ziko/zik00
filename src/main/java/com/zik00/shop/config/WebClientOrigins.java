package com.zik00.shop.config;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

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

        try {
            URI uri = new URI(value.strip());
            String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
            String path = uri.getPath();
            if (!("http".equals(scheme) || "https".equals(scheme))
                    || uri.getHost() == null
                    || uri.getUserInfo() != null
                    || uri.getQuery() != null
                    || uri.getFragment() != null
                    || (path != null && !path.isEmpty() && !"/".equals(path))) {
                throw new IllegalArgumentException("Web client URL must be an HTTP(S) origin without a path.");
            }
            return new URI(scheme, null, uri.getHost(), uri.getPort(), null, null, null).toASCIIString();
        } catch (URISyntaxException exception) {
            throw new IllegalArgumentException("Web client origin is invalid.", exception);
        }
    }
}
