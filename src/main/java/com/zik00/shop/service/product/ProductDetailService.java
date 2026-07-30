package com.zik00.shop.service.product;

import com.zik00.shop.domain.product.KreamCatalogReview;
import com.zik00.shop.domain.search.KreamCatalogProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.dto.product.ProductReviewResponse;
import com.zik00.shop.repository.search.KreamProductCatalogRepository;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Service
public class ProductDetailService {
    private final KreamProductCatalogRepository productCatalogRepository;

    public ProductDetailService(KreamProductCatalogRepository productCatalogRepository) {
        this.productCatalogRepository = productCatalogRepository;
    }

    public Optional<ProductDetailResponse> findById(String productId) {
        return productCatalogRepository.findByProductId(productId).map(this::toResponse);
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
                product.rating(),
                product.reviewCount(),
                product.reviews().stream().map(this::toReviewResponse).toList(),
                product.tags()
        );
    }

    private ProductReviewResponse toReviewResponse(KreamCatalogReview review) {
        return new ProductReviewResponse(
                review.reviewId(),
                review.reviewType(),
                review.author(),
                review.content(),
                review.createdAt(),
                review.rating(),
                review.likeCount(),
                review.images().stream().map(this::proxyImageUrl).toList(),
                review.reviewUrl()
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
