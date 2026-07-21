package com.zik00.shop.config;

import java.io.IOException;
import java.util.Map;

import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.OAuthAccountService;
import com.zik00.shop.service.auth.RegistrationService;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class OAuthLoginSuccessHandler implements AuthenticationSuccessHandler {
    private static final Logger log = LoggerFactory.getLogger(OAuthLoginSuccessHandler.class);
    private final OAuthAccountService oauthAccountService;
    private final RegistrationService registrationService;
    private final String frontendBaseUrl;
    private final OAuthLoginCompletionService loginCompletionService;

    public OAuthLoginSuccessHandler(
            OAuthAccountService oauthAccountService,
            RegistrationService registrationService,
            OAuthLoginCompletionService loginCompletionService,
            WebClientOrigins webClientOrigins
    ) {
        this.oauthAccountService = oauthAccountService;
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
        OAuth2User oauthUser = oauthAuthentication.getPrincipal();
        String provider = oauthAuthentication.getAuthorizedClientRegistrationId();
        String subject = authentication.getName();
        String email = oauthEmail(provider, oauthUser);
        if (!isValidEmail(email)) {
            log.warn("OAuth provider {} did not return email. Attribute keys: {}",
                    provider, oauthUser.getAttributes().keySet());
            invalidateSession(request);
            response.sendRedirect(frontendBaseUrl
                    + "/login?error&reason=oauth-email-missing&provider=" + provider);
            return;
        }
        User existingUser = oauthAccountService.findExisting(provider, subject).orElse(null);
        String completionCode;
        if (existingUser != null && registrationService.isRegistrationComplete(existingUser)) {
            oauthAccountService.saveEmailIfMissing(existingUser, email);
            completionCode = loginCompletionService.prepareExisting(existingUser);
        } else {
            completionCode = loginCompletionService.prepareRegistration(
                    provider,
                    subject,
                    email,
                    oauthName(provider, oauthUser)
            );
        }
        invalidateSession(request);
        response.sendRedirect(frontendBaseUrl + "/oauth/callback?code=" + completionCode);
    }

    private String oauthEmail(String provider, OAuth2User oauthUser) {
        if ("kakao".equals(provider)) {
            return nestedText(oauthUser.getAttribute("kakao_account"), "email");
        }
        if (oauthUser instanceof OidcUser oidcUser) {
            String idTokenEmail = text(oidcUser.getIdToken().getClaim("email"));
            if (!idTokenEmail.isBlank()) {
                return idTokenEmail;
            }
        }
        return text(oauthUser.getAttribute("email"));
    }

    private boolean isValidEmail(String email) {
        int at = email.indexOf('@');
        return email.length() <= 255 && at > 0 && at < email.length() - 1;
    }

    private void invalidateSession(HttpServletRequest request) {
        if (request.getSession(false) != null) {
            request.getSession(false).invalidate();
        }
    }

    private String oauthName(String provider, OAuth2User oauthUser) {
        if ("kakao".equals(provider)) {
            String nickname = nestedText(oauthUser.getAttribute("kakao_account"), "profile", "nickname");
            if (!nickname.isBlank()) {
                return nickname;
            }
            return nestedText(oauthUser.getAttribute("properties"), "nickname");
        }
        if ("line".equals(provider)) {
            String name = text(oauthUser.getAttribute("name"));
            return name.isBlank() ? text(oauthUser.getAttribute("displayName")) : name;
        }
        return text(oauthUser.getAttribute("name"));
    }

    private String nestedText(Object value, String... path) {
        Object current = value;
        for (String key : path) {
            if (!(current instanceof Map<?, ?> values)) {
                return "";
            }
            current = values.get(key);
        }
        return text(current);
    }

    private String text(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
