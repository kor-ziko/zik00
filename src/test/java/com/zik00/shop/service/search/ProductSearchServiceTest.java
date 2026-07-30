package com.zik00.shop.service.search;

import com.zik00.shop.dto.search.SearchResultResponse;
import com.zik00.shop.repository.search.KreamProductCatalogRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductSearchServiceTest {
    private final KreamProductCatalogRepository kreamRepository =
            new KreamProductCatalogRepository("item_data/kream_output.json");
    private final ProductSearchService productSearchService = new ProductSearchService(kreamRepository);

    @Test
    void searchesProductsByNameAndCategory() {
        SearchResultResponse result = productSearchService.search(
                "텀블러", null, List.of(), null, null, "relevance", 0, 20
        );

        assertTrue(result.totalCount() >= 1);
        assertTrue(result.items().stream().allMatch(product -> product.name().contains("텀블러")));
        assertTrue(result.items().stream().anyMatch(product -> product.productId().equals("3")));
    }

    @Test
    void searchesTheFeaturedProductShownOnTheHomePage() {
        SearchResultResponse result = productSearchService.search(
                "폴로", null, List.of(), null, null, "relevance", 0, 20
        );

        long expectedCount = kreamRepository.findAll().stream()
                .filter(product -> product.name().contains("폴로"))
                .count();
        assertEquals(expectedCount, result.totalCount());
        assertTrue(result.items().stream().allMatch(product -> product.name().contains("폴로")));
    }

    @Test
    void appliesBrandPriceAndSortFilters() {
        SearchResultResponse result = productSearchService.search(
                "", null, List.of("ZIK:00"), 3_000L, 7_000L, "price-high", 0, 20
        );

        assertTrue(result.items().stream().allMatch(product ->
                product.price() >= 3_000L && product.price() <= 7_000L
        ));
        for (int index = 1; index < result.items().size(); index++) {
            assertTrue(result.items().get(index - 1).price() >= result.items().get(index).price());
        }
    }

    @Test
    void limitsResultsToTheSelectedTopLevelCategory() {
        SearchResultResponse result = productSearchService.search(
                "", "뷰티·미용", List.of(), null, null, "relevance", 0, 20
        );

        assertTrue(result.totalCount() > 0);
        assertTrue(result.items().stream()
                .allMatch(product -> product.category().startsWith("뷰티·미용")));
    }

    @Test
    void capsPageSize() {
        SearchResultResponse result = productSearchService.search(
                "", null, List.of(), null, null, "relevance", 0, 100
        );

        assertEquals(40, result.size());
    }
}
