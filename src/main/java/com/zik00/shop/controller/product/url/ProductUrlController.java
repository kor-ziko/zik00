package com.zik00.shop.controller.product.url;

import com.zik00.shop.dto.product.url.ProductUrlResolveResponse;
import com.zik00.shop.service.product.url.ProductUrlImportService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/product-links")
public class ProductUrlController {
    private final ProductUrlImportService productUrlImportService;

    public ProductUrlController(ProductUrlImportService productUrlImportService) {
        this.productUrlImportService = productUrlImportService;
    }

    @GetMapping("/resolve")
    public ProductUrlResolveResponse resolve(@RequestParam String url) {
        try {
            return productUrlImportService.resolve(url);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        } catch (IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, exception.getMessage(), exception);
        }
    }
}
