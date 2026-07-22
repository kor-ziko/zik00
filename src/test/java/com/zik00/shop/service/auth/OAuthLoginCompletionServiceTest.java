package com.zik00.shop.service.auth;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.zik00.shop.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class OAuthLoginCompletionServiceTest {

    @Test
    void rejectsCompletionCodeThatIsNotBoundToTheBrowserSession() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        OAuthLoginCompletionService service = new OAuthLoginCompletionService(
                mock(UserRepository.class),
                mock(JwtSessionService.class),
                mock(JwtCookieService.class),
                mock(PendingOAuthRegistrationService.class),
                redisTemplate,
                new OpaqueTokenCodec()
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        service.bindToNewSession("legitimate-code", request);

        assertThrows(
                InvalidJwtException.class,
                () -> service.complete("stolen-code", request, new MockHttpServletResponse())
        );
        verify(redisTemplate, never()).opsForValue();
    }
}
