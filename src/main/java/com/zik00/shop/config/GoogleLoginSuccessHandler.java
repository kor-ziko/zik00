package com.zik00.shop.config;

import java.io.IOException;

import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.AuthenticatedUserService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final AuthenticatedUserService authenticatedUserService;
    private final RegistrationService registrationService;
    private final String frontendBaseUrl;
    private final OAuthLoginCompletionService loginCompletionService;

    public GoogleLoginSuccessHandler(
            AuthenticatedUserService authenticatedUserService,
            RegistrationService registrationService,
            OAuthLoginCompletionService loginCompletionService,
            WebClientOrigins webClientOrigins
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.registrationService = registrationService;
        this.loginCompletionService = loginCompletionService;
        this.frontendBaseUrl = webClientOrigins.clientBaseUrl();
    }

    @Override
    public void onAuthenticationSuccess(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) throws IOException, ServletException {
        User user = authenticatedUserService.findGoogleUser(authentication.getName());
        String destination = registrationService.isRegistrationComplete(user)
                ? "/"
                : "/login/additional-info";
        String completionCode = loginCompletionService.prepare(user, destination);
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(frontendBaseUrl + "/oauth/callback?code=" + completionCode);
    }
}
