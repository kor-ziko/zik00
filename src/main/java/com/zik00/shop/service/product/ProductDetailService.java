package com.zik00.shop.service.product;

import com.zik00.shop.domain.search.KreamCatalogProduct;
import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.repository.search.KreamProductCatalogRepository;
import com.zik00.shop.service.product.extractor.ProductPageExtractor;
import com.zik00.shop.service.search.ExternalProductCatalog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProductDetailService {
    private final KreamProductCatalogRepository productCatalogRepository;
    private final ExternalProductCatalog externalProductCatalog;
    private final ProductPageExtractor productPageExtractor;

    public ProductDetailService(KreamProductCatalogRepository productCatalogRepository) {
        this(productCatalogRepository, ExternalProductCatalog.disabled(), null);
    }

    @Autowired
    public ProductDetailService(
            KreamProductCatalogRepository productCatalogRepository,
            ExternalProductCatalog externalProductCatalog,
            ProductPageExtractor productPageExtractor
    ) {
        this.productCatalogRepository = productCatalogRepository;
        this.externalProductCatalog = externalProductCatalog;
        this.productPageExtractor = productPageExtractor;
    }

    public Optional<ProductDetailResponse> findById(String productId) {
        Optional<ProductDetailResponse> cached = externalProductCatalog.findDetail(productId);
        if (cached.isPresent()) return cached;
        Optional<KreamCatalogProduct> local = productCatalogRepository.findByProductId(productId);
        if (local.isPresent()) {
            ProductDetailResponse fallback = toResponse(local.get());
            if (productPageExtractor == null || local.get().sourceUrl() == null || local.get().sourceUrl().isBlank()) {
                return Optional.of(fallback);
            }
            try {
                ProductDetailResponse detail = productPageExtractor.extract(toDiscoveredProduct(local.get()));
                externalProductCatalog.saveDetail(detail);
                return Optional.of(detail);
            } catch (RuntimeException ignored) {
                return Optional.of(fallback);
            }
        }
        if (productPageExtractor == null) return Optional.empty();
        return externalProductCatalog.findProduct(productId).map(product -> {
            ProductDetailResponse detail = productPageExtractor.extract(externalProductCatalog.resolveMerchant(product));
            externalProductCatalog.saveDetail(detail);
            return detail;
        });
    }

    private DiscoveredProduct toDiscoveredProduct(KreamCatalogProduct product) {
        long sellingPrice = product.discountedPrice() > 0 ? product.discountedPrice() : product.price();
        Long originalPrice = product.price() > sellingPrice ? product.price() : null;
        return new DiscoveredProduct(
                product.productId(), product.productId(), null, product.name(), product.category(),
                product.brand(), sellingPrice, originalPrice, product.currency(), product.sourceUrl(),
                product.thumbnailUrl(), product.rating(), product.reviewCount(), "KREAM", product.description(), Map.of()
        );
    }

    private ProductDetailResponse toResponse(KreamCatalogProduct product) {
        long sellingPrice = product.discountedPrice() > 0 ? product.discountedPrice() : product.price();
        Long originalPrice = product.price() > sellingPrice ? product.price() : null;
        List<String> images = product.images().stream().map(this::proxyImageUrl).toList();
        String thumbnail = proxyImageUrl(product.thumbnailUrl());

        return new ProductDetailResponse(
                product.productId(),
                product.sourceUrl(),
                product.name(),
                product.category(),
                sellingPrice,
                originalPrice,
                thumbnail,
                images.isEmpty() ? List.of(thumbnail) : images,
                product.brand(),
                product.description(),
                product.currency(),
                3_000L,
                true,
                product.rating(),
                product.reviewCount(),
                List.of(),
                List.of(),
                product.tags()
        );
    }

    private String proxyImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "/assets/product-shoes.webp";
        }
        return "/api/product-images/proxy?url="
                + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
    }
}
