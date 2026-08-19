package com.zik00.shop.service.product.pricing;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
public class CustomsProductClassifier {
    private static final Logger log = LoggerFactory.getLogger(CustomsProductClassifier.class);
    private static final List<String> GROUPS = List.of("2", "3", "4", "5", "6", "7");

    private final boolean aiEnabled;
    private final String baseUrl;
    private final String model;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public CustomsProductClassifier(
            @Value("${shop.product-discovery.ai-provider:disabled}") String provider,
            @Value("${shop.product-discovery.ollama-base-url:http://localhost:11434}") String baseUrl,
            @Value("${shop.product-discovery.ollama-model:qwen2.5:7b}") String model,
            ObjectMapper objectMapper
    ) {
        this.aiEnabled = "ollama".equalsIgnoreCase(provider);
        this.baseUrl = baseUrl.replaceAll("/+$", "");
        this.model = model;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(3)).build();
    }

    public Classification classify(String productName, String category) {
        Classification rules = rules(productName, category);
        if (!aiEnabled) return rules;
        try {
            Classification ai = classifyWithAi(productName, category);
            if (ai == null || ai.confidence() < 0.70 || !GROUPS.contains(ai.simplifiedTariffGroup())) return rules;
            return new Classification(
                    ai.simplifiedTariffGroup(),
                    rules.smallValueExemptionExcluded() || ai.smallValueExemptionExcluded(),
                    rules.generalTariffRequired() || ai.generalTariffRequired(),
                    rules.hsCodeCandidate().isBlank() ? ai.hsCodeCandidate() : rules.hsCodeCandidate(),
                    ai.confidence(), "AI_ASSISTED"
            );
        } catch (Exception exception) {
            log.info("관세 상품 AI 분류를 건너뛰고 규칙 분류를 사용합니다: {}", exception.getMessage());
            return rules;
        }
    }

    private Classification classifyWithAi(String productName, String category) throws Exception {
        String prompt = """
                일본으로 개인 수입되는 한국 상품을 관세 분류하세요.
                명시된 사실만 사용하고 재질이나 원산지를 추측하지 마세요. JSON 객체 하나만 반환하세요.
                필드: simplifiedTariffGroup, smallValueExemptionExcluded, generalTariffRequired,
                hsCodeCandidate, confidence
                simplifiedTariffGroup은 일본 소액수입 간이세율 2~7 중 하나입니다.
                2=모피제품 등, 3=커피·차 등, 4=비니트 의류,
                5=플라스틱·유리·비금속·가구, 6=고무·종이·도자기·철강, 7=기타입니다.
                니트 의류, 신발, 가죽 가방, 귀금속처럼 간이세율 제외품목이면
                generalTariffRequired를 true로 반환하세요.
                hsCodeCandidate는 확실한 경우에만 4~6자리로 반환하고 아니면 빈 문자열입니다.
                confidence는 0~1입니다.

                상품명: %s
                카테고리: %s
                """.formatted(productName, category);
        String body = objectMapper.writeValueAsString(Map.of(
                "model", model, "stream", false, "format", "json",
                "options", Map.of("temperature", 0, "num_predict", 300),
                "messages", List.of(Map.of("role", "user", "content", prompt))
        ));
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + "/api/chat"))
                .timeout(Duration.ofSeconds(45)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8)).build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) return null;
        JsonNode envelope = objectMapper.readTree(response.body());
        JsonNode result = objectMapper.readTree(envelope.path("message").path("content").asString("{}"));
        return new Classification(
                result.path("simplifiedTariffGroup").asString("7"),
                result.path("smallValueExemptionExcluded").asBoolean(false),
                result.path("generalTariffRequired").asBoolean(false),
                result.path("hsCodeCandidate").asString(""),
                result.path("confidence").asDouble(0), "AI_ASSISTED"
        );
    }

    static Classification rules(String productName, String category) {
        String text = (productName + " " + category).toLowerCase(Locale.ROOT);
        String group = "7";
        if (contains(text, "모피", "fur", "아이스크림", "케첩")) group = "2";
        else if (contains(text, "커피", "차", "tea", "coffee", "젤라틴", "접착제")) group = "3";
        else if (contains(text, "식품", "푸드", "과자", "의류", "셔츠", "팬츠", "스커트", "원피스")) group = "4";
        else if (contains(text, "뷰티", "화장품", "향수", "플라스틱", "가구", "침구", "완구", "스포츠", "캠핑")) group = "5";
        else if (contains(text, "종이", "문구", "도서", "세라믹", "도자기", "고무", "철강")) group = "6";
        boolean exemptionExcluded = contains(text, "가죽 가방", "가죽가방", "가죽 장갑", "가죽장갑", "니트", "스웨터",
                "스키부츠", "가죽 신발", "가죽신발", "가죽 구두", "가죽구두");
        boolean generalTariffRequired = contains(text, "가죽", "니트", "스웨터", "신발", "슈즈", "부츠", "샌들", "슬리퍼",
                "로퍼", "스니커즈", "구두", "주얼리", "쥬얼리");
        String hsCodeCandidate = contains(text, "스웨터", "케이블 니트", "풀오버", "가디건") ? "6110" : "";
        return new Classification(group, exemptionExcluded, generalTariffRequired, hsCodeCandidate, 1, "RULE");
    }

    private static boolean contains(String text, String... candidates) {
        for (String candidate : candidates) if (text.contains(candidate)) return true;
        return false;
    }

    public record Classification(
            String simplifiedTariffGroup,
            boolean smallValueExemptionExcluded,
            boolean generalTariffRequired,
            String hsCodeCandidate,
            double confidence,
            String method
    ) {}
}
