package com.zik00.shop.repository.search;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;
import tools.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class DiscoveredProductCacheRepository {
    private static final Logger log = LoggerFactory.getLogger(DiscoveredProductCacheRepository.class);
    private static final String SEARCH_PREFIX = "shop:product-discovery:search:v2:";
    private static final String PRODUCT_PREFIX = "shop:product-discovery:product:v2:";
    private static final String DETAIL_PREFIX = "shop:product-discovery:detail:v25:";

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Duration searchTtl;
    private final Duration detailTtl;
    private final ConcurrentHashMap<String, String> memoryFallback = new ConcurrentHashMap<>();

    public DiscoveredProductCacheRepository(
            StringRedisTemplate redisTemplate,
            @Value("${shop.product-discovery.search-cache-ttl:P1D}") Duration searchTtl,
            @Value("${shop.product-discovery.detail-cache-ttl:P7D}") Duration detailTtl
    ) {
        this.redisTemplate = redisTemplate;
        this.searchTtl = searchTtl;
        this.detailTtl = detailTtl;
    }

    public Optional<List<DiscoveredProduct>> findSearch(String query, String category) {
        return read(searchKey(query, category), DiscoveredProduct[].class).map(Arrays::asList);
    }

    public void saveSearch(String query, String category, List<DiscoveredProduct> products) {
        write(searchKey(query, category), products, searchTtl);
        products.forEach(product -> write(PRODUCT_PREFIX + product.productId(), product, detailTtl));
    }

    public Optional<DiscoveredProduct> findProduct(String productId) {
        return read(PRODUCT_PREFIX + productId, DiscoveredProduct.class);
    }

    public void saveProduct(DiscoveredProduct product) {
        write(PRODUCT_PREFIX + product.productId(), product, detailTtl);
    }

    public Optional<ProductDetailResponse> findDetail(String productId) {
        return read(DETAIL_PREFIX + productId, ProductDetailResponse.class);
    }

    public void saveDetail(ProductDetailResponse detail) {
        write(DETAIL_PREFIX + detail.id(), detail, detailTtl);
    }

    private <T> Optional<T> read(String key, Class<T> type) {
        try {
            String value = redisTemplate.opsForValue().get(key);
            if (value == null) value = memoryFallback.get(key);
            return value == null ? Optional.empty() : Optional.of(objectMapper.readValue(value, type));
        } catch (Exception exception) {
            log.warn("상품 캐시를 읽지 못했습니다: {}", key, exception);
            return Optional.empty();
        }
    }

    private void write(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            memoryFallback.put(key, json);
            redisTemplate.opsForValue().set(key, json, ttl);
        } catch (Exception exception) {
            log.warn("상품 캐시를 Redis에 저장하지 못해 메모리 캐시만 사용합니다: {}", key, exception);
        }
    }

    private String searchKey(String query, String category) {
        return SEARCH_PREFIX + sha256((query == null ? "" : query) + "\n" + (category == null ? "" : category));
    }

    private String sha256(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            return java.util.HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", exception);
        }
    }
}
