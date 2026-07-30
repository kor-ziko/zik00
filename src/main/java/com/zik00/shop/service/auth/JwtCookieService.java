package com.zik00.shop.service.auth;

import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class JwtCookieService {
    public static final String ACCESS_COOKIE = "zik_access_token";
    public static final String REFRESH_COOKIE = "zik_refresh_token";
    private static final String LEGACY_ACCESS_COOKIE = "access_token";
    private static final String LEGACY_REFRESH_COOKIE = "refresh_token";
    private static final String ROOT_PATH = "/";
    private static final String LEGACY_AUTH_PATH = "/api/auth";
    private static final String LEGACY_REFRESH_PATH = "/api/auth/refresh";
    private static final String[] TOKEN_COOKIE_NAMES = {
            ACCESS_COOKIE,
            REFRESH_COOKIE,
            LEGACY_ACCESS_COOKIE,
            LEGACY_REFRESH_COOKIE
    };
    private static final String[] TOKEN_COOKIE_PATHS = {
            ROOT_PATH,
            LEGACY_AUTH_PATH,
            LEGACY_REFRESH_PATH
    };

    private final boolean secure;
    private final Duration refreshCookieTtl;

    public JwtCookieService(
            @Value("${shop.jwt.cookie-secure:false}") boolean secure,
            @Value("${shop.jwt.refresh-cookie-ttl:PT30M}") Duration refreshCookieTtl
    ) {
        if (refreshCookieTtl.isZero() || refreshCookieTtl.isNegative()) {
            throw new IllegalArgumentException("Refresh Token cookie TTL must be positive");
        }
        this.secure = secure;
        this.refreshCookieTtl = refreshCookieTtl;
    }

    public void writeTokenPair(HttpServletResponse response, JwtService.JwtPair pair) {
        Duration accessRemaining = Duration.between(java.time.Instant.now(), pair.accessExpiresAt());
        Duration tokenRemaining = Duration.between(java.time.Instant.now(), pair.refreshExpiresAt());
        Duration cookieMaxAge = tokenRemaining.compareTo(refreshCookieTtl) < 0
                ? tokenRemaining
                : refreshCookieTtl;
        addCookie(response, ACCESS_COOKIE, pair.accessToken(), accessRemaining, ROOT_PATH);
        addCookie(response, REFRESH_COOKIE, pair.refreshToken(), cookieMaxAge, ROOT_PATH);
    }

    public Optional<String> readAccessToken(HttpServletRequest request) {
        return readCookie(request, ACCESS_COOKIE);
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_COOKIE);
    }

    public void clearRefreshToken(HttpServletResponse response) {
        clearTokenCookies(response);
    }

    private Optional<String> readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private void clearTokenCookies(HttpServletResponse response) {
        for (String name : TOKEN_COOKIE_NAMES) {
            for (String path : TOKEN_COOKIE_PATHS) {
                addCookie(response, name, "", Duration.ZERO, path);
            }
        }
    }

    private void addCookie(
            HttpServletResponse response,
            String name,
            String value,
            Duration maxAge,
            String path
    ) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

}
