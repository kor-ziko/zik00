package com.zik00.shop.service.search;

import com.zik00.shop.domain.search.KreamCatalogProduct;
import com.zik00.shop.dto.search.SearchFacetResponse;
import com.zik00.shop.dto.search.SearchProductResponse;
import com.zik00.shop.dto.search.SearchResultResponse;
import com.zik00.shop.repository.search.KreamProductCatalogRepository;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class ProductSearchService {
    private static final int MAX_PAGE_SIZE = 40;
    private static final List<SearchProductResponse> LOCAL_PRODUCTS = List.of(
            new SearchProductResponse("2", "충전식 무선 미니 테이블 팬",
                    "가전 > 계절가전", "LUMENA", 4190L, 5200L, "JPY",
                    null, "/assets/product-mini-fan.webp", 4.7, 347, true, "ZIK:00", "급상승"),
            new SearchProductResponse("3", "24시간 보냉 와이드 텀블러",
                    "생활잡화 > 주방용품", "LOCK&LOCK", 3650L, null, "JPY",
                    null, "/assets/product-tumbler.webp", 4.8, 134, true, "ZIK:00", "베스트"),
            new SearchProductResponse("4", "라이트 데님 서머 메쉬 캡",
                    "패션의류 > 패션잡화", "MARDI MERCREDI", 2890L, null, "JPY",
                    null, "/assets/product-summer-cap.webp", 4.6, 48, false, "ZIK:00", "신상품"),
            new SearchProductResponse("5", "플랫폼 스트랩 서머 샌들",
                    "패션의류 > 신발", "SAPPUN", 6790L, 8100L, "JPY",
                    null, "/assets/product-sandals.webp", 4.7, 92, false, "ZIK:00", "서울 픽")
    );

    private final KreamProductCatalogRepository kreamProductCatalogRepository;

    public ProductSearchService(KreamProductCatalogRepository kreamProductCatalogRepository) {
        this.kreamProductCatalogRepository = kreamProductCatalogRepository;
    }

    public SearchResultResponse search(
            String query,
            String category,
            List<String> brands,
            Long minPrice,
            Long maxPrice,
            String sort,
            int page,
            int size
    ) {
        return search(query, "all", category, brands, minPrice, maxPrice, sort, page, size);
    }

    public SearchResultResponse search(
            String query,
            String scope,
            String category,
            List<String> brands,
            Long minPrice,
            Long maxPrice,
            String sort,
            int page,
            int size
    ) {
        String normalizedQuery = normalize(query);
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);

        List<SearchCandidate> queryMatches = allCandidates()
                .filter(candidate -> matchesQuery(candidate, normalizedQuery, scope))
                .toList();

        List<SearchFacetResponse> categories = buildFacets(
                queryMatches.stream().map(SearchCandidate::product).toList(),
                product -> product.category().split(" > ", 2)[0]
        );
        List<SearchFacetResponse> brandFacets = buildFacets(
                queryMatches.stream().map(SearchCandidate::product).toList(),
                SearchProductResponse::brand
        );

        List<SearchProductResponse> filtered = queryMatches.stream()
                .map(SearchCandidate::product)
                .filter(product -> category == null || category.isBlank() || product.category().startsWith(category))
                .filter(product -> brands == null || brands.isEmpty() || brands.contains(product.brand()))
                .filter(product -> minPrice == null || product.price() >= minPrice)
                .filter(product -> maxPrice == null || product.price() <= maxPrice)
                .sorted(resolveSort(sort))
                .toList();

        int fromIndex = Math.min(safePage * safeSize, filtered.size());
        int toIndex = Math.min(fromIndex + safeSize, filtered.size());
        int totalPages = filtered.isEmpty() ? 0 : (int) Math.ceil((double) filtered.size() / safeSize);

        return new SearchResultResponse(
                query == null ? "" : query.trim(),
                filtered.size(),
                safePage,
                safeSize,
                totalPages,
                filtered.subList(fromIndex, toIndex),
                categories,
                brandFacets
        );
    }

    private Stream<SearchCandidate> allCandidates() {
        return Stream.concat(
                kreamProductCatalogRepository.findAll().stream()
                        .map(product -> new SearchCandidate(toSearchProduct(product), product.description())),
                LOCAL_PRODUCTS.stream().map(product -> new SearchCandidate(product, ""))
        );
    }

    private SearchProductResponse toSearchProduct(KreamCatalogProduct product) {
        long sellingPrice = product.discountedPrice() > 0 ? product.discountedPrice() : product.price();
        Long originalPrice = product.price() > sellingPrice ? product.price() : null;
        String badge = product.available()
                ? product.discountRate() > 0 ? product.discountRate() + "% 할인" : null
                : "품절";

        return new SearchProductResponse(
                product.productId(),
                product.name(),
                product.category(),
                product.brand().isBlank() ? "브랜드 정보 없음" : product.brand(),
                sellingPrice,
                originalPrice,
                product.currency().isBlank() ? "KRW" : product.currency(),
                product.sourceUrl(),
                proxyImageUrl(product.thumbnailUrl()),
                product.rating() == null ? 0.0 : product.rating(),
                product.reviewCount(),
                false,
                "KREAM",
                badge
        );
    }

    private String proxyImageUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "/assets/product-shoes.webp";
        }
        return "/api/product-images/proxy?url="
                + URLEncoder.encode(imageUrl, StandardCharsets.UTF_8);
    }

    private boolean matchesQuery(SearchCandidate candidate, String normalizedQuery, String scope) {
        if (normalizedQuery.isBlank()) {
            return true;
        }
        SearchProductResponse product = candidate.product();
        Stream<String> searchableValues = switch (normalize(scope)) {
            case "title" -> Stream.of(product.name());
            case "title-content" -> Stream.of(product.name(), candidate.description());
            default -> Stream.of(
                    product.name(),
                    candidate.description(),
                    product.productId(),
                    product.category(),
                    product.brand(),
                    product.source(),
                    product.sourceUrl()
            );
        };
        return searchableValues
                .map(this::normalize)
                .anyMatch(value -> value.contains(normalizedQuery));
    }

    private List<SearchFacetResponse> buildFacets(
            List<SearchProductResponse> products,
            Function<SearchProductResponse, String> classifier
    ) {
        return products.stream()
                .collect(Collectors.groupingBy(classifier, Collectors.counting()))
                .entrySet()
                .stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .map(entry -> new SearchFacetResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    private Comparator<SearchProductResponse> resolveSort(String sort) {
        return switch (sort == null ? "" : sort.toLowerCase(Locale.ROOT)) {
            case "price-low" -> Comparator.comparingLong(SearchProductResponse::price);
            case "price-high" -> Comparator.comparingLong(SearchProductResponse::price).reversed();
            case "reviews" -> Comparator.comparingInt(SearchProductResponse::reviewCount).reversed();
            case "rating" -> Comparator.comparingDouble(SearchProductResponse::rating).reversed();
            default -> Comparator.comparing((SearchProductResponse product) -> product.badge() == null)
                    .thenComparing(Comparator.comparingInt(SearchProductResponse::reviewCount).reversed());
        };
    }

    private String normalize(String value) {
        return value == null ? "" : value.strip().toLowerCase(Locale.ROOT);
    }

    private record SearchCandidate(SearchProductResponse product, String description) {
    }
}
