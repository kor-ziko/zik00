package com.zik00.shop.config;

import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

public class OAuthReauthenticationRequestResolver implements OAuth2AuthorizationRequestResolver {
    private static final String AUTHORIZATION_BASE_URI = "/oauth2/authorization";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public OAuthReauthenticationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository,
                AUTHORIZATION_BASE_URI
        );
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        return customize(delegate.resolve(request), registrationId(request));
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        return customize(delegate.resolve(request, clientRegistrationId), clientRegistrationId);
    }

    private OAuth2AuthorizationRequest customize(
            OAuth2AuthorizationRequest authorizationRequest,
            String registrationId
    ) {
        if (authorizationRequest == null) {
            return null;
        }
        String prompt = switch (registrationId) {
            case "google" -> "select_account";
            case "kakao" -> "login";
            case "line" -> "consent";
            default -> null;
        };
        if (prompt == null) {
            return authorizationRequest;
        }

        Map<String, Object> additionalParameters =
                new LinkedHashMap<>(authorizationRequest.getAdditionalParameters());
        if (prompt != null) {
            additionalParameters.put("prompt", prompt);
        }
        return OAuth2AuthorizationRequest.from(authorizationRequest)
                .additionalParameters(additionalParameters)
                .build();
    }

    private String registrationId(HttpServletRequest request) {
        String path = request.getServletPath();
        String prefix = AUTHORIZATION_BASE_URI + "/";
        return path.startsWith(prefix) ? path.substring(prefix.length()) : "";
    }
}
