package com.zik00.shop.service.product.extractor;

import com.zik00.shop.domain.search.DiscoveredProduct;
import com.zik00.shop.dto.product.ProductDetailResponse;
import com.zik00.shop.service.product.ProductImageSourceRegistry;
import org.jsoup.Jsoup;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class ProductPageExtractorTest {

    @Test
    void extractsOptionsFromNextData() {
        String html = """
                <html><head><script id="__NEXT_DATA__" type="application/json">
                {"props":{"product":{"name":"나이키 테스트 운동화","brandName":"NIKE",
                "sellPrice":102000,"originalSellPrice":109000,"deliveryFee":2500,
                "thumbnails":[
                  {"url":"sitem.ssgcdn.com/01/33/49/item/test_i1_1200.jpg"},
                  {"url":"https://sitem.ssgcdn.com/01/33/49/item/test_i1_250.jpg"}
                ],
                "option":{"label":["옵션"],"items":[
                  {"id":1,"attributes":["250"],"stock":1,"price":0,"status":"SALE"},
                  {"id":2,"attributes":["255"],"stock":0,"price":3000,"status":"SALE"}
                ]}}}}
                </script></head><body></body></html>
                """;
        RenderedProductPageFetcher pageFetcher = new RenderedProductPageFetcher(false) {
            @Override
            public synchronized Optional<org.jsoup.nodes.Document> fetch(String sourceUrl) {
                return Optional.of(Jsoup.parse(html, sourceUrl));
            }
        };
        ProductPageExtractor extractor = new ProductPageExtractor(
                new OllamaProductMetadataExtractor("disabled", "http://localhost:11434", "unused"),
                new ProductImageSourceRegistry(),
                pageFetcher
        );
        DiscoveredProduct product = new DiscoveredProduct(
                "TEST-1", "TEST-1", null, "운동화", "신발", "브랜드", 50_000,
                null, "KRW", "https://example.com/product", "", null, 0,
                "TEST", "", Map.of()
        );

        ProductDetailResponse detail = extractor.extract(product);

        assertThat(detail.name()).isEqualTo("나이키 테스트 운동화");
        assertThat(detail.brand()).isEqualTo("NIKE");
        assertThat(detail.price()).isEqualTo(102_000L);
        assertThat(detail.originalPrice()).isEqualTo(109_000L);
        assertThat(detail.domesticShippingFee()).isEqualTo(2_500L);
        assertThat(detail.domesticShippingFeeEstimated()).isFalse();
        assertThat(detail.images()).singleElement().satisfies(image -> assertThat(image)
                .contains("https%3A%2F%2Fsitem.ssgcdn.com%2F01%2F33%2F49%2Fitem%2Ftest_i1_1200.jpg")
                .doesNotContain("test_i1_250.jpg"));
        assertThat(detail.options()).singleElement().satisfies(option -> {
            assertThat(option.optionType()).isEqualTo("사이즈");
            assertThat(option.values()).containsExactly("250", "255");
        });
        assertThat(detail.variants()).hasSize(2);
        assertThat(detail.variants().get(0).available()).isTrue();
        assertThat(detail.variants().get(1).available()).isFalse();
        assertThat(detail.variants().get(1).price()).isEqualTo(105_000L);
    }
}
