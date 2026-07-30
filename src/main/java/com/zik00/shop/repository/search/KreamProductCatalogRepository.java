package com.zik00.shop.repository.search;

import com.zik00.shop.domain.product.KreamCatalogReview;
import com.zik00.shop.domain.search.KreamCatalogProduct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Repository
public class KreamProductCatalogRepository {
    private final Path catalogPath;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private volatile List<KreamCatalogProduct> cachedProducts = List.of();
    private volatile long cachedModifiedTime = Long.MIN_VALUE;

    public KreamProductCatalogRepository(
            @Value("${search.catalog.kream-path:item_data/kream_output.json}") String catalogPath
    ) {
        this.catalogPath = Path.of(catalogPath).toAbsolutePath().normalize();
    }

    public List<KreamCatalogProduct> findAll() {
        try {
            long modifiedTime = Files.getLastModifiedTime(catalogPath).toMillis();
            if (modifiedTime != cachedModifiedTime) {
                reload(modifiedTime);
            }
            return cachedProducts;
        } catch (IOException exception) {
            throw new IllegalStateException("KREAM 상품 데이터 파일을 읽을 수 없습니다: " + catalogPath, exception);
        }
    }

    public Optional<KreamCatalogProduct> findByProductId(String productId) {
        return findAll().stream()
                .filter(product -> product.productId().equals(productId))
                .findFirst();
    }

    private synchronized void reload(long modifiedTime) {
        if (modifiedTime == cachedModifiedTime) {
            return;
        }
        try {
            JsonNode root = objectMapper.readTree(Files.readString(catalogPath));
            if (!root.isArray()) {
                throw new IllegalStateException("KREAM 상품 데이터는 JSON 배열이어야 합니다.");
            }
            cachedProducts = root.values().stream()
                    .map(this::toProduct)
                    .filter(product -> !product.productId().isBlank() && !product.name().isBlank())
                    .toList();
            cachedModifiedTime = modifiedTime;
        } catch (IOException | JacksonException exception) {
            throw new IllegalStateException("KREAM 상품 데이터 형식이 올바르지 않습니다: " + catalogPath, exception);
        }
    }

    private KreamCatalogProduct toProduct(JsonNode item) {
        return new KreamCatalogProduct(
                item.path("productId").asString(),
                item.path("sourceUrl").asString(),
                item.path("name").asString(),
                item.path("category").asString("기타"),
                item.path("brand").asString("브랜드 정보 없음"),
                item.path("description").asString(),
                item.path("price").asLong(),
                item.path("discountedPrice").asLong(),
                item.path("currency").asString("KRW"),
                item.path("discountRate").asInt(),
                item.path("isAvailable").asBoolean(true),
                item.path("thumbnailUrl").asString(),
                stringList(item.path("images")),
                item.path("rating").isNumber() ? item.path("rating").asDouble() : null,
                item.path("reviewCount").asInt(),
                item.path("reviews").values().stream().map(this::toReview).toList(),
                stringList(item.path("tags"))
        );
    }

    private KreamCatalogReview toReview(JsonNode review) {
        return new KreamCatalogReview(
                review.path("reviewId").asString(),
                review.path("reviewType").asString(),
                review.path("author").asString(),
                review.path("content").asString(),
                review.path("createdAt").asString(),
                review.path("rating").isNumber() ? review.path("rating").asDouble() : null,
                review.path("likeCount").asInt(),
                stringList(review.path("images")),
                review.path("reviewUrl").asString()
        );
    }

    private List<String> stringList(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        return node.values().stream()
                .map(JsonNode::asString)
                .filter(value -> !value.isBlank())
                .toList();
    }
}
