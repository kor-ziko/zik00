package com.zik00.shop.controller.search;

import com.zik00.shop.dto.search.SearchResultResponse;
import com.zik00.shop.service.search.ProductSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/search")
public class ProductSearchController {
    private final ProductSearchService productSearchService;

    public ProductSearchController(ProductSearchService productSearchService) {
        this.productSearchService = productSearchService;
    }

    @GetMapping
    public SearchResultResponse search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "all") String scope,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) List<String> brand,
            @RequestParam(required = false) Long minPrice,
            @RequestParam(required = false) Long maxPrice,
            @RequestParam(defaultValue = "relevance") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return productSearchService.search(q, scope, category, brand, minPrice, maxPrice, sort, page, size);
    }
}
