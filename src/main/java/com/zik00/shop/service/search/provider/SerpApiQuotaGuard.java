package com.zik00.shop.service.search.provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Component
public class SerpApiQuotaGuard {
    private static final Logger log = LoggerFactory.getLogger(SerpApiQuotaGuard.class);

    private final String apiKey;
    private final int reserve;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private Instant checkedAt = Instant.EPOCH;
    private int estimatedRemaining;

    public SerpApiQuotaGuard(
            @Value("${shop.product-discovery.serpapi-key:}") String apiKey,
            @Value("${shop.product-discovery.quota-reserve:10}") int reserve
    ) {
        this.apiKey = apiKey;
        this.reserve = Math.max(0, reserve);
        this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    }

    public synchronized boolean tryAcquire() {
        if (apiKey.isBlank()) return false;
        if (checkedAt.plus(Duration.ofMinutes(5)).isBefore(Instant.now())) refresh();
        if (estimatedRemaining <= reserve) {
            log.warn("SerpApi 무료 호출량 보호를 위해 외부 검색을 중단합니다. 남은 호출 추정치: {}", estimatedRemaining);
            return false;
        }
        estimatedRemaining--;
        return true;
    }

    private void refresh() {
        try {
            URI uri = URI.create("https://serpapi.com/account.json?api_key="
                    + URLEncoder.encode(apiKey, StandardCharsets.UTF_8));
            HttpRequest request = HttpRequest.newBuilder(uri)
                    .timeout(Duration.ofSeconds(10))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                estimatedRemaining = 0;
                return;
            }
            JsonNode account = objectMapper.readTree(response.body());
            estimatedRemaining = account.path("total_searches_left")
                    .asInt(account.path("plan_searches_left").asInt(0));
            checkedAt = Instant.now();
        } catch (Exception exception) {
            estimatedRemaining = 0;
            checkedAt = Instant.now();
            log.warn("SerpApi 남은 호출량을 확인하지 못해 외부 호출을 중단합니다.", exception);
        }
    }
}
