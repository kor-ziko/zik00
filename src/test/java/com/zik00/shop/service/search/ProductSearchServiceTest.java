package com.zik00.shop.service.search;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.dto.search.SearchResultResponse;
import com.zik00.shop.repository.search.KreamProductCatalogRepository;
import com.zik00.shop.service.product.ProductImageSourceRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

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

        assertEquals(30, result.size());
    }

    @Test
    void distinguishesTitleSearchFromTitleAndDescriptionSearch() {
        SearchResultResponse titleOnly = productSearchService.search(
                "정품 검수 완료", "title", null, List.of(), null, null, "relevance", 0, 20
        );
        SearchResultResponse titleAndDescription = productSearchService.search(
                "정품 검수 완료", "title-content", null, List.of(), null, null, "relevance", 0, 20
        );

        assertEquals(0, titleOnly.totalCount());
        assertTrue(titleAndDescription.totalCount() > 0);
    }

    @Test
    void filtersByTheFullCategoryPathSelectedFromTheMegaMenu() {
        SearchResultResponse result = productSearchService.search(
                "", "all", "패션의류 > 여성의류 > 여성 상의 > 여성 니트",
                List.of(), null, null, "relevance", 0, 20
        );

        assertTrue(result.totalCount() > 0);
        assertTrue(result.items().stream().allMatch(product ->
                product.category().startsWith("패션의류 > 여성의류 > 여성 상의 > 여성 니트")
        ));
    }

    @Test
    void mergesExternalProductsWhenOnlyCategoryIsSelected() {
        AtomicReference<String> requestedCategory = new AtomicReference<>();
        ExternalProductCatalog externalCatalog = new ExternalProductCatalog() {
            @Override
            public List<DiscoveredProduct> search(String query, String category) {
                requestedCategory.set(category);
                return List.of(new DiscoveredProduct(
                        "SERP-category-product", "provider-id", null, "외부 패션 상품",
                        category, "외부 브랜드", 39_000L, null, "KRW",
                        "https://shop.example/product", "https://shop.example/product.jpg",
                        4.5, 10, "Google Shopping", "", Map.of()
                ));
            }

            @Override public Optional<DiscoveredProduct> findProduct(String productId) { return Optional.empty(); }
            @Override public void saveProduct(DiscoveredProduct product) { }
            @Override public Optional<ProductDetailResponse> findDetail(String productId) { return Optional.empty(); }
            @Override public void saveDetail(ProductDetailResponse detail) { }
            @Override public DiscoveredProduct resolveMerchant(DiscoveredProduct product) { return product; }
        };
        ProductSearchService service = new ProductSearchService(
                kreamRepository, externalCatalog, new ProductImageSourceRegistry()
        );

        SearchResultResponse result = service.search(
                "", "all", "패션의류", List.of(), null, null, "relevance", 0, 5
        );

        assertEquals("패션의류", requestedCategory.get());
        assertTrue(result.totalCount() > 0);
        assertTrue(result.brands().stream().anyMatch(facet -> facet.value().equals("외부 브랜드")));
        assertTrue(result.items().stream().anyMatch(product -> product.productId().equals("SERP-category-product")));
    }
}
