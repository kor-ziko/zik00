package com.zik00.shop.config;

import java.io.IOException;

import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.GoogleAccountService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class GoogleLoginSuccessHandler implements AuthenticationSuccessHandler {
    private final GoogleAccountService googleAccountService;
    private final RegistrationService registrationService;
    private final String frontendBaseUrl;
    private final OAuthLoginCompletionService loginCompletionService;

    public GoogleLoginSuccessHandler(
            GoogleAccountService googleAccountService,
            RegistrationService registrationService,
            OAuthLoginCompletionService loginCompletionService,
            WebClientOrigins webClientOrigins
    ) {
        this.googleAccountService = googleAccountService;
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
        OAuth2User oauthUser = (OAuth2User) authentication.getPrincipal();
        String subject = authentication.getName();
        User existingUser = googleAccountService.findExisting(subject).orElse(null);
        String completionCode;
        if (existingUser != null && registrationService.isRegistrationComplete(existingUser)) {
            completionCode = loginCompletionService.prepareExisting(existingUser);
        } else {
            completionCode = loginCompletionService.prepareRegistration(
                    subject,
                    oauthUser.getAttribute("email"),
                    oauthUser.getAttribute("name")
            );
        }
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
        response.sendRedirect(frontendBaseUrl + "/oauth/callback?code=" + completionCode);
    }
}
