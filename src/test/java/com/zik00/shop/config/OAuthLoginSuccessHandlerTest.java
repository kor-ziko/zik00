package com.zik00.shop.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.shop.domain.User;
import com.zik00.shop.service.auth.OAuthAccountService;
import com.zik00.shop.service.auth.OAuthLoginCompletionService;
import com.zik00.shop.service.auth.OAuthProfile;
import com.zik00.shop.service.auth.OAuthProfileExtractor;
import com.zik00.shop.service.auth.RegistrationService;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;

class OAuthLoginSuccessHandlerTest {
    @Test
    void withdrawnMemberIsSentToRegistrationInsteadOfExistingLogin() throws Exception {
        OAuthAccountService accountService = mock(OAuthAccountService.class);
        OAuthProfileExtractor profileExtractor = mock(OAuthProfileExtractor.class);
        RegistrationService registrationService = mock(RegistrationService.class);
        OAuthLoginCompletionService completionService = mock(OAuthLoginCompletionService.class);
        WebClientOrigins origins = mock(WebClientOrigins.class);
        when(origins.clientBaseUrl()).thenReturn("http://localhost:5174");
        OAuthLoginSuccessHandler handler = new OAuthLoginSuccessHandler(
                accountService,
                profileExtractor,
                registrationService,
                completionService,
                origins
        );
        OAuth2AuthenticationToken authentication = mock(OAuth2AuthenticationToken.class);
        OAuthProfile profile = new OAuthProfile("google", "subject", "member@example.com", "회원");
        User user = new User(
                "회원", LocalDate.of(1990, 1, 1), "M", "닉네임", "03-1111-1111",
                "google:subject", 0, 0, "090-1111-1111", "member@example.com", 0,
                LocalDate.now(), "일반회원", false
        );
        user.changeMemberStatus("WITHDRAWN");
        when(profileExtractor.extract(authentication)).thenReturn(profile);
        when(accountService.findExistingAndBackfillEmail(profile)).thenReturn(Optional.of(user));
        when(registrationService.isRegistrationComplete(user)).thenReturn(true);
        when(completionService.prepareRegistration(profile)).thenReturn("registration-code");
        MockHttpServletRequest request = new MockHttpServletRequest();
        MockHttpServletResponse response = new MockHttpServletResponse();

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(completionService).prepareRegistration(profile);
        verify(completionService, never()).prepareExisting(user);
        verify(completionService).bindToNewSession("registration-code", request);
        assertThat(response.getRedirectedUrl())
                .isEqualTo("http://localhost:5174/oauth/callback#code=registration-code");
    }
}
