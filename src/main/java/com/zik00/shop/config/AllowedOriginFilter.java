package com.zik00.shop.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Set;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.web.filter.OncePerRequestFilter;

public class AllowedOriginFilter extends OncePerRequestFilter {
    private static final String OAUTH_AUTHORIZATION_START = "/oauth2/authorization/";
    private static final String OAUTH_CALLBACK = "/login/oauth2/code/";
    private static final byte[] FORBIDDEN_RESPONSE =
            "{\"message\":\"허용되지 않은 요청 출처입니다.\"}".getBytes(StandardCharsets.UTF_8);

    private final Set<String> allowedOrigins;

    public AllowedOriginFilter(Collection<String> allowedOrigins) {
        this.allowedOrigins = Set.copyOf(allowedOrigins);
        if (this.allowedOrigins.isEmpty()) {
            throw new IllegalArgumentException("At least one web client origin is required.");
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (path.startsWith(OAUTH_AUTHORIZATION_START)
                || path.startsWith(OAUTH_CALLBACK)
                || comesFromAllowedOrigin(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentLength(FORBIDDEN_RESPONSE.length);
        response.setHeader("Cache-Control", "no-store");
        response.getOutputStream().write(FORBIDDEN_RESPONSE);
    }

    private boolean comesFromAllowedOrigin(HttpServletRequest request) {
        String origin = request.getHeader("Origin");
        if (origin != null) {
            return allowedOrigins.contains(origin);
        }

        String referer = request.getHeader("Referer");
        if (referer == null) {
            return false;
        }
        for (String allowedOrigin : allowedOrigins) {
            if (referer.equals(allowedOrigin) || referer.startsWith(allowedOrigin + "/")) {
                return true;
            }
        }
        return false;
    }
}
