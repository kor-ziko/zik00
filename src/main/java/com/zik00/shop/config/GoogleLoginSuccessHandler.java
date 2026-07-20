package com.zik00.shop.config;

import java.io.IOException;

import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.JwtSessionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthenticatedUserService authenticatedUserService;
    private final RegistrationService registrationService;
    private final String frontendBaseUrl;
    private final JwtSessionService jwtSessionService;

    public GoogleLoginSuccessHandler(
            AuthenticatedUserService authenticatedUserService,
            RegistrationService registrationService,
            JwtSessionService jwtSessionService,
            @Value("${shop.frontend.base-url:http://localhost:5174}") String frontendBaseUrl
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.registrationService = registrationService;
        this.jwtSessionService = jwtSessionService;
        this.frontendBaseUrl = frontendBaseUrl.replaceAll("/+$", "");
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        User user = authenticatedUserService.findGoogleUser(authentication.getName());
        jwtSessionService.issue(user, response);
        String destination = registrationService.isRegistrationComplete(user)
                ? "/"
                : "/login/additional-info";
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(frontendBaseUrl + destination);
    }
}
