package com.zik00.shop.service.search;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;

import java.util.List;
import java.util.Optional;

public interface ExternalProductCatalog {
    List<DiscoveredProduct> search(String query, String category);
    Optional<DiscoveredProduct> findProduct(String productId);
    void saveProduct(DiscoveredProduct product);
    Optional<ProductDetailResponse> findDetail(String productId);
    void saveDetail(ProductDetailResponse detail);
    DiscoveredProduct resolveMerchant(DiscoveredProduct product);

    static ExternalProductCatalog disabled() {
        return new ExternalProductCatalog() {
            public List<DiscoveredProduct> search(String query, String category) { return List.of(); }
            public Optional<DiscoveredProduct> findProduct(String productId) { return Optional.empty(); }
            public void saveProduct(DiscoveredProduct product) { }
            public Optional<ProductDetailResponse> findDetail(String productId) { return Optional.empty(); }
            public void saveDetail(ProductDetailResponse detail) { }
            public DiscoveredProduct resolveMerchant(DiscoveredProduct product) { return product; }
        };
    }
}
