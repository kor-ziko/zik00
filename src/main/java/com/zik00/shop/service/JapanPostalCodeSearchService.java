package com.zik00.shop.service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.zik00.shop.dto.mypage.JapanPostalCodeResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JapanPostalCodeSearchService {
    private static final String ZIPCLOUD_BASE_URL = "https://zipcloud.ibsnet.co.jp";
    private static final Duration CACHE_TTL = Duration.ofHours(12);
    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration READ_TIMEOUT = Duration.ofSeconds(3);
    private static final int MAX_CACHE_SIZE = 2048;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public JapanPostalCodeSearchService() {
        this.restClient = RestClient.builder()
                .baseUrl(ZIPCLOUD_BASE_URL)
                .requestFactory(requestFactory())
                .build();
    }

    public List<JapanPostalCodeResponse> findByPostalCode(String postalCode) {
        String normalizedPostalCode = normalizePostalCode(postalCode);
        if (normalizedPostalCode.length() != 7) {
            return List.of();
        }

        Instant now = Instant.now();
        CacheEntry cached = cache.get(normalizedPostalCode);
        if (cached != null && cached.isFresh(now)) {
            return cached.results();
        }

        List<JapanPostalCodeResponse> results = fetchPostalCode(normalizedPostalCode);
        cache.put(normalizedPostalCode, new CacheEntry(results, now.plus(CACHE_TTL)));
        pruneCache(now);
        return results;
    }

    private List<JapanPostalCodeResponse> fetchPostalCode(String postalCode) {
        JsonNode response = search(postalCode);
        if (response == null || response.path("results").isNull()) {
            return List.of();
        }
        if (response.path("status").asInt() != 200) {
            throw new IllegalStateException(response.path("message").asString());
        }

        return response.path("results").values().stream()
                .map(result -> new JapanPostalCodeResponse(
                        result.path("zipcode").asString(),
                        result.path("address1").asString(),
                        normalize(result.path("address2").asString()) + normalize(result.path("address3").asString())
                ))
                .toList();
    }

    private SimpleClientHttpRequestFactory requestFactory() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(CONNECT_TIMEOUT);
        requestFactory.setReadTimeout(READ_TIMEOUT);
        return requestFactory;
    }

    private JsonNode search(String postalCode) {
        try {
            String responseBody = restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/api/search")
                            .queryParam("zipcode", postalCode)
                            .build())
                    .retrieve()
                    .body(String.class);
            return objectMapper.readTree(responseBody);
        } catch (RestClientException exception) {
            throw new IllegalStateException("zipcloud postal code lookup failed.", exception);
        } catch (JacksonException exception) {
            throw new IllegalStateException("zipcloud postal code response was invalid.", exception);
        }
    }

    private String normalizePostalCode(String value) {
        return value == null ? "" : value.replaceAll("\\D", "");
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private void pruneCache(Instant now) {
        if (cache.size() <= MAX_CACHE_SIZE) {
            return;
        }
        cache.entrySet().removeIf(entry -> !entry.getValue().isFresh(now));
        int excessCount = cache.size() - MAX_CACHE_SIZE;
        if (excessCount <= 0) {
            return;
        }
        for (String key : cache.keySet()) {
            if (excessCount-- <= 0) {
                return;
            }
            cache.remove(key);
        }
    }

    private record CacheEntry(
            List<JapanPostalCodeResponse> results,
            Instant expiresAt
    ) {
        private boolean isFresh(Instant now) {
            return now.isBefore(expiresAt);
        }
    }

}
