package com.zik00.shop.service.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class OAuthUserService extends DefaultOAuth2UserService {
    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        Object identifier = switch (registrationId) {
            case "google" -> oauthUser.getAttribute("sub");
            case "kakao" -> oauthUser.getAttribute("id");
            case "line" -> oauthUser.getAttribute("userId");
            default -> null;
        };
        String subject = identifier == null ? null : identifier.toString();
        if (subject == null || subject.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"));
        }
        return oauthUser;
    }
}
