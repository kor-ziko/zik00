package com.zik00.shop.service.search;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.repository.search.DiscoveredProductCacheRepository;
import com.zik00.shop.service.search.provider.ProductSearchProvider;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ExternalProductSearchService implements ExternalProductCatalog {
    private final ProductSearchProvider provider;
    private final DiscoveredProductCacheRepository cacheRepository;

    public ExternalProductSearchService(
            ProductSearchProvider provider,
            DiscoveredProductCacheRepository cacheRepository
    ) {
        this.provider = provider;
        this.cacheRepository = cacheRepository;
    }

    @Override
    public List<DiscoveredProduct> search(String query, String category) {
        boolean queryMissing = query == null || query.isBlank();
        boolean categoryMissing = category == null || category.isBlank();
        if (queryMissing && categoryMissing) return List.of();
        return cacheRepository.findSearch(query, category).orElseGet(() -> {
            List<DiscoveredProduct> products = provider.search(query, category);
            if (!products.isEmpty()) cacheRepository.saveSearch(query, category, products);
            return products;
        });
    }

    @Override
    public Optional<DiscoveredProduct> findProduct(String productId) {
        return cacheRepository.findProduct(productId);
    }

    @Override
    public void saveProduct(DiscoveredProduct product) {
        cacheRepository.saveProduct(product);
    }

    @Override
    public Optional<ProductDetailResponse> findDetail(String productId) {
        return cacheRepository.findDetail(productId);
    }

    @Override
    public void saveDetail(ProductDetailResponse detail) {
        cacheRepository.saveDetail(detail);
    }

    @Override
    public DiscoveredProduct resolveMerchant(DiscoveredProduct product) {
        DiscoveredProduct resolved = provider.resolveMerchant(product);
        cacheRepository.saveProduct(resolved);
        return resolved;
    }
}
