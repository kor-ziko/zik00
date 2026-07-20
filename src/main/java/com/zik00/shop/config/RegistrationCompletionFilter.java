package com.zik00.shop.config;

import java.io.IOException;

import com.zik00.shop.service.auth.RegistrationService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class RegistrationCompletionFilter extends OncePerRequestFilter {
    private final RegistrationService registrationService;

    public RegistrationCompletionFilter(RegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (request.getRequestURI().startsWith(request.getContextPath() + "/mypage")
                && authentication != null
                && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())
                && !registrationService.isCurrentUserRegistrationComplete()) {
            response.sendRedirect(request.getContextPath() + "/signup/detail");
            return;
        }
        filterChain.doFilter(request, response);
    }
}
