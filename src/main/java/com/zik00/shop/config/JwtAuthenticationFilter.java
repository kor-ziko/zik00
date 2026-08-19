package com.zik00.shop.config;

import java.io.IOException;
import java.util.List;

import com.zik00.shop.repository.UserRepository;
import com.zik00.shop.service.auth.InvalidJwtException;
import com.zik00.shop.service.auth.JwtCookieService;
import com.zik00.shop.service.auth.JwtService;
import com.zik00.shop.service.auth.RedisRefreshTokenStore;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.http.HttpHeaders;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private static final String ADMIN_API_PREFIX = "/api/admin/";
    private final JwtService jwtService;
    private final JwtCookieService cookieService;
    private final UserRepository userRepository;
    private final RedisRefreshTokenStore refreshTokenStore;
    private final SecurityContextRepository securityContextRepository;

    public JwtAuthenticationFilter(
            JwtService jwtService,
            JwtCookieService cookieService,
            UserRepository userRepository,
            RedisRefreshTokenStore refreshTokenStore,
            SecurityContextRepository securityContextRepository
    ) {
        this.jwtService = jwtService;
        this.cookieService = cookieService;
        this.userRepository = userRepository;
        this.refreshTokenStore = refreshTokenStore;
        this.securityContextRepository = securityContextRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getServletPath().startsWith(ADMIN_API_PREFIX);
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cookieService.readAccessToken(request)
                    .or(() -> readBearerToken(request))
                    .ifPresent(token -> authenticate(token, request, response));
        }
        filterChain.doFilter(request, response);
    }

    private java.util.Optional<String> readBearerToken(HttpServletRequest request) {
        String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return java.util.Optional.empty();
        }
        String token = authorization.substring(7).trim();
        return token.isEmpty() ? java.util.Optional.empty() : java.util.Optional.of(token);
    }

    private void authenticate(String token, HttpServletRequest request, HttpServletResponse response) {
        try {
            JwtService.JwtClaims claims = jwtService.validate(token, JwtService.ACCESS);
            if (refreshTokenStore.isAccessTokenRevoked(claims.tokenId())) {
                return;
            }
            userRepository.findByAccessId(claims.accessId())
                    .filter(user -> "ACTIVE".equals(user.getMemberStatus()))
                    .ifPresent(user -> {
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        user.getAccessId(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );
                authentication.setDetails(request.getRemoteAddr());
                SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
                securityContext.setAuthentication(authentication);
                SecurityContextHolder.setContext(securityContext);
                securityContextRepository.saveContext(securityContext, request, response);
                    });
        } catch (InvalidJwtException ignored) {
            // 만료되거나 변조된 Access Token은 인증에 사용하지 않는다.
        }
    }
}
