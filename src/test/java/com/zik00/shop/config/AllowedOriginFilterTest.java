package com.zik00.shop.config;

import jakarta.servlet.ServletException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AllowedOriginFilterTest {
    private final AllowedOriginFilter filter = new AllowedOriginFilter(List.of(
            "http://localhost:5174",
            "http://127.0.0.1:5173"
    ));

    @Test
    void allowsClientOrigin() throws ServletException, IOException {
        assertAllowed("Origin", "http://localhost:5174", "/api/auth/session");
    }

    @Test
    void allowsAdminOrigin() throws ServletException, IOException {
        assertAllowed("Origin", "http://127.0.0.1:5173", "/api/admin/members");
    }

    @Test
    void allowsConfiguredFrontendReferer() throws ServletException, IOException {
        assertAllowed("Referer", "http://localhost:5174/login", "/oauth2/authorization/google");
    }

    @Test
    void allowsConfiguredAdminReferer() throws ServletException, IOException {
        assertAllowed("Referer", "http://127.0.0.1:5173/members", "/api/admin/members");
    }

    @Test
    void blocksMissingOrForeignOrigin() throws ServletException, IOException {
        MockHttpServletRequest missingOrigin = new MockHttpServletRequest("GET", "/api/auth/session");
        MockHttpServletResponse missingOriginResponse = new MockHttpServletResponse();
        filter.doFilter(missingOrigin, missingOriginResponse, new MockFilterChain());

        MockHttpServletRequest foreignOrigin = new MockHttpServletRequest("GET", "/api/auth/session");
        foreignOrigin.addHeader("Origin", "http://localhost:5172");
        MockHttpServletResponse foreignOriginResponse = new MockHttpServletResponse();
        filter.doFilter(foreignOrigin, foreignOriginResponse, new MockFilterChain());

        assertThat(missingOriginResponse.getStatus()).isEqualTo(403);
        assertThat(foreignOriginResponse.getStatus()).isEqualTo(403);
    }

    @Test
    void doesNotTrustAllowedRefererWhenOriginIsForeign() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/session");
        request.addHeader("Origin", "http://localhost:5172");
        request.addHeader("Referer", "http://localhost:5174/login");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(403);
    }

    @Test
    void allowsGoogleOAuthCallbackWithoutBrowserOrigin() throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/login/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }

    private void assertAllowed(String headerName, String headerValue, String path)
            throws ServletException, IOException {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        request.addHeader(headerName, headerValue);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(200);
    }
}
