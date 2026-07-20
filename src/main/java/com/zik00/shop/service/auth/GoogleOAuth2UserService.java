package com.zik00.shop.service.auth;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
public class GoogleOAuth2UserService extends DefaultOAuth2UserService {
    private final GoogleAccountService googleAccountService;
    private final OAuthAccessTokenService accessTokenService;

    public GoogleOAuth2UserService(
            GoogleAccountService googleAccountService,
            OAuthAccessTokenService accessTokenService
    ) {
        this.googleAccountService = googleAccountService;
        this.accessTokenService = accessTokenService;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauthUser = super.loadUser(userRequest);
        if (!"google".equals(userRequest.getClientRegistration().getRegistrationId())) {
            return oauthUser;
        }

        String subject = oauthUser.getAttribute("sub");
        if (subject == null || subject.isBlank()) {
            throw new OAuth2AuthenticationException(new OAuth2Error("invalid_user_info"));
        }
        var user = googleAccountService.findOrCreate(
                subject,
                oauthUser.getAttribute("email"),
                oauthUser.getAttribute("name")
        );
        accessTokenService.saveGoogleAccessToken(user, userRequest.getAccessToken());
        return oauthUser;
    }
}
