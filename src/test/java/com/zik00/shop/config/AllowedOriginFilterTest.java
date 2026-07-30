package com.zik00.shop.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;

import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class AllowedOriginFilterTest {
    private final AllowedOriginFilter filter =
            new AllowedOriginFilter(List.of("http://localhost:5174"));

    @Test
    void rejectsProductImageWithoutOriginHeaders() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/product-images/proxy");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw new AssertionError("Filter chain must not be called.");
        };

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }

    @Test
    void allowsProductImageFromConfiguredFrontend() throws Exception {
        MockHttpServletRequest request =
                new MockHttpServletRequest("GET", "/api/product-images/proxy");
        request.addHeader("Referer", "http://localhost:5174/products/b70e8ae9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) ->
                ((MockHttpServletResponse) servletResponse).setStatus(204);

        filter.doFilter(request, response, chain);

        assertEquals(204, response.getStatus());
    }

    @Test
    void stillRejectsOtherRequestsWithoutOriginHeaders() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/mypage");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (servletRequest, servletResponse) -> {
            throw new AssertionError("Filter chain must not be called.");
        };

        filter.doFilter(request, response, chain);

        assertEquals(403, response.getStatus());
    }
}
