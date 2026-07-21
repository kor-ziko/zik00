package com.zik00.shop.service.auth;

import java.util.Map;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;

@Component
public class OAuthProfileExtractor {
    public OAuthProfile extract(OAuth2AuthenticationToken authentication) {
        OAuth2User user = authentication.getPrincipal();
        OAuthProvider provider = OAuthProvider.from(authentication.getAuthorizedClientRegistrationId());
        return new OAuthProfile(
                provider.registrationId(),
                authentication.getName(),
                email(provider, user),
                ""
        );
    }

    private String email(OAuthProvider provider, OAuth2User user) {
        if (provider == OAuthProvider.KAKAO) {
            return nestedText(user.getAttribute("kakao_account"), "email");
        }
        if (user instanceof OidcUser oidcUser) {
            String idTokenEmail = text(oidcUser.getIdToken().getClaim("email"));
            if (!idTokenEmail.isBlank()) {
                return idTokenEmail;
            }
        }
        return text(user.getAttribute("email"));
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
