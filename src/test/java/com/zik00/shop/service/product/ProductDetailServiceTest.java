package com.zik00.shop.service.product;

import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.repository.search.KreamProductCatalogRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductDetailServiceTest {
    private final KreamProductCatalogRepository repository =
            new KreamProductCatalogRepository("item_data/kream_output.json");
    private final ProductDetailService service = new ProductDetailService(repository);

    @Test
    void findsAProductWithItsDetailData() {
        String productId = repository.findAll().getFirst().productId();

        ProductDetailResponse product = service.findById(productId).orElseThrow();

        assertEquals(productId, product.id());
        assertFalse(product.name().isBlank());
        assertFalse(product.images().isEmpty());
        assertTrue(product.image().startsWith("/api/product-images/proxy"));
    }

    @Test
    void returnsEmptyForAnUnknownProduct() {
        assertTrue(service.findById("KREAM-NOT-FOUND").isEmpty());
    }
}
