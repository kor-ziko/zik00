package com.zik00.shop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

class OAuthReauthenticationRequestResolverTest {

    @Test
    void addsS256PkceToConfidentialClientAuthorizationRequests() {
        ClientRegistration registration = ClientRegistration.withRegistrationId("google")
                .clientId("client-id")
                .clientSecret("client-secret")
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.example/authorize")
                .tokenUri("https://accounts.example/token")
                .build();
        OAuthReauthenticationRequestResolver resolver = new OAuthReauthenticationRequestResolver(
                new InMemoryClientRegistrationRepository(registration)
        );
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/oauth2/authorization/google");
        request.setServletPath("/oauth2/authorization/google");
        request.setScheme("https");
        request.setServerName("shop.example");
        request.setServerPort(443);

        OAuth2AuthorizationRequest authorizationRequest = resolver.resolve(request);

        assertNotNull(authorizationRequest);
        assertNotNull(authorizationRequest.getAttribute("code_verifier"));
        assertNotNull(authorizationRequest.getAdditionalParameters().get("code_challenge"));
        assertEquals("S256", authorizationRequest.getAdditionalParameters().get("code_challenge_method"));
        assertEquals("select_account", authorizationRequest.getAdditionalParameters().get("prompt"));
    }
}
