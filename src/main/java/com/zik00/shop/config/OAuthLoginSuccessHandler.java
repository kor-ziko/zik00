package com.zik00.shop.config;

import java.io.IOException;

import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.OAuthAccountService;
import com.zik00.shop.service.auth.OAuthProfile;
import com.zik00.shop.service.auth.OAuthProfileExtractor;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);
    private final OAuthAccountService oauthAccountService;
    private final OAuthProfileExtractor profileExtractor;
    private final RegistrationService registrationService;
    private final String frontendBaseUrl;
    private final OAuthLoginCompletionService loginCompletionService;

    public OAuthLoginSuccessHandler(
            OAuthAccountService oauthAccountService,
            OAuthProfileExtractor profileExtractor,
            RegistrationService registrationService,
            OAuthLoginCompletionService loginCompletionService,
            WebClientOrigins webClientOrigins
    ) {
        this.oauthAccountService = oauthAccountService;
        this.profileExtractor = profileExtractor;
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
        OAuth2AuthenticationToken oauthAuthentication = (OAuth2AuthenticationToken) authentication;
        OAuthProfile profile = profileExtractor.extract(oauthAuthentication);
        if (!profile.hasValidEmail()) {
            log.warn("OAuth provider {} did not return email. Attribute keys: {}",
                    profile.provider(), oauthAuthentication.getPrincipal().getAttributes().keySet());
            invalidateSession(request);
            response.sendRedirect(frontendBaseUrl
                    + "/login?error&reason=oauth-email-missing&provider=" + profile.provider());
            return;
        }
        User existingUser = oauthAccountService.findExistingAndBackfillEmail(profile).orElse(null);
        String completionCode;
        if (existingUser != null && registrationService.isRegistrationComplete(existingUser)) {
            completionCode = loginCompletionService.prepareExisting(existingUser);
        } else {
            completionCode = loginCompletionService.prepareRegistration(profile);
        }
        invalidateSession(request);
        response.sendRedirect(frontendBaseUrl + "/oauth/callback?code=" + completionCode);
    }

    private void invalidateSession(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

}
