package com.zik00.shop.service.product.extractor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class OllamaProductMetadataExtractor {
    private static final Logger log = LoggerFactory.getLogger(OllamaProductMetadataExtractor.class);
    private static final int MAX_PAGE_TEXT = 24_000;

    private final boolean enabled;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OllamaProductMetadataExtractor(
            @Value("${shop.product-discovery.ai-provider:disabled}") String provider,
            @Value("${shop.product-discovery.ollama-base-url:http://localhost:11434}") String baseUrl,
            @Value("${shop.product-discovery.ollama-model:qwen2.5:7b}") String model
    ) {
        this.enabled = "ollama".equalsIgnoreCase(provider);
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public Optional<ExtractedProductMetadata> extract(String pageText, String knownName) {
        if (!enabled || pageText == null || pageText.isBlank()) return Optional.empty();
        String truncated = pageText.substring(0, Math.min(pageText.length(), MAX_PAGE_TEXT));
        String prompt = """
                다음은 온라인 쇼핑 상품 페이지에서 추출한 텍스트입니다.
                확인 가능한 값만 사용하고 추측하지 마세요. 모르는 값은 빈 문자열 또는 null로 반환하세요.
                반드시 설명 없이 JSON 객체 하나만 반환하세요.
                필드: name, brand, description, price, originalPrice, currency, domesticShippingFee, image, images, options, variants
                price와 originalPrice는 통화 기호와 쉼표가 없는 정수, images는 URL 문자열 배열입니다.
                domesticShippingFee는 국내 배송비 정수이며 무료배송은 0, 확인할 수 없으면 null입니다.
                options는 [{"optionType":"색상","values":["검정","흰색"]}] 형식입니다.
                variants는 [{"variantId":"SKU-1","attributes":{"색상":"검정","사이즈":"260"},"price":129000,"available":true}] 형식입니다.
                옵션 조합별 가격이나 재고가 확인되지 않으면 variants를 빈 배열로 반환하고 절대 만들어내지 마세요.
                신고, 리뷰, 사진/동영상, 광고, 욕설/비방, 정렬, 배송 관련 입력 항목은 상품 옵션이 아닙니다.

                알려진 상품명: %s
                페이지 텍스트:
                %s
                """.formatted(knownName, truncated);
        try {
            String requestJson = objectMapper.writeValueAsString(Map.of(
                    "model", model,
                    "stream", false,
                    "format", "json",
                    "options", Map.of("temperature", 0, "num_predict", 2048),
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            ));
            HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                    .timeout(Duration.ofSeconds(45))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) return Optional.empty();
            JsonNode envelope = objectMapper.readTree(response.body());
            String content = envelope.path("message").path("content").asString();
            if (content.isBlank()) return Optional.empty();
            return Optional.of(objectMapper.readValue(content, ExtractedProductMetadata.class));
        } catch (Exception exception) {
            log.info("Ollama 상품 정보 보완을 건너뜁니다: {}", exception.getMessage());
            return Optional.empty();
        }
    }
}
