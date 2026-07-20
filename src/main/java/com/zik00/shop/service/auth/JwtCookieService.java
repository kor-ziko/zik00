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
    public static final String REFRESH_COOKIE = "zik_refresh_token";
    private static final String OLD_ACCESS_COOKIE = "zik_access_token";
    private static final String LEGACY_ACCESS_COOKIE = "access_token";
    private static final String LEGACY_REFRESH_COOKIE = "refresh_token";

    private final boolean secure;

    public JwtCookieService(@Value("${shop.jwt.cookie-secure:false}") boolean secure) {
        this.secure = secure;
    }

    public void writeRefreshToken(HttpServletResponse response, JwtService.JwtPair pair) {
        addCookie(response, OLD_ACCESS_COOKIE, "", Duration.ZERO);
        addCookie(response, LEGACY_ACCESS_COOKIE, "", Duration.ZERO);
        addCookie(response, LEGACY_REFRESH_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_COOKIE, pair.refreshToken(), Duration.between(java.time.Instant.now(), pair.refreshExpiresAt()));
    }

    public Optional<String> readRefreshToken(HttpServletRequest request) {
        return readCookie(request, REFRESH_COOKIE);
    }

    public void clearRefreshToken(HttpServletResponse response) {
        addCookie(response, OLD_ACCESS_COOKIE, "", Duration.ZERO);
        addCookie(response, REFRESH_COOKIE, "", Duration.ZERO);
        addCookie(response, LEGACY_ACCESS_COOKIE, "", Duration.ZERO);
        addCookie(response, LEGACY_REFRESH_COOKIE, "", Duration.ZERO);
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

    private void addCookie(HttpServletResponse response, String name, String value, Duration maxAge) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge.isNegative() ? Duration.ZERO : maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
