package com.zik00.shop.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URI;

import com.zik00.shop.service.product.ProductImageSourceRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class ProductImageProxyServiceTest {
    private final ProductImageProxyService service = new ProductImageProxyService();

    @Test
    void acceptsKreamImageHostOverHttps() {
        URI source = service.validateSource(
                "https://kream-phinf.pstatic.net/path/image.jpeg?type=l_webp"
        );

        assertEquals("kream-phinf.pstatic.net", source.getHost());
    }

    @Test
    void rejectsUnknownHosts() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> service.validateSource("https://example.com/image.jpg")
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    @Test
    void acceptsAnImageUrlRegisteredByProductSearch() {
        ProductImageSourceRegistry registry = new ProductImageSourceRegistry();
        registry.register("https://images.example.com/products/123.jpg");
        ProductImageProxyService registeredService = new ProductImageProxyService(registry);

        URI source = registeredService.validateSource("https://images.example.com/products/123.jpg");

        assertEquals("images.example.com", source.getHost());
    }

    @Test
    void rejectsHostNamePrefixTrick() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.validateSource(
                        "https://kream-phinf.pstatic.net.evil.example/image.jpg"
                )
        );
    }

    @Test
    void rejectsNonHttpsUrls() {
        assertThrows(
                ResponseStatusException.class,
                () -> service.validateSource(
                        "http://kream-phinf.pstatic.net/image.jpg"
                )
        );
    }
}
