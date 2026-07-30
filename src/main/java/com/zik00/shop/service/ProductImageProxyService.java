package com.zik00.shop.service;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductImageProxyService {
    private static final Set<String> ALLOWED_HOSTS = Set.of("kream-phinf.pstatic.net");
    private static final int MAX_IMAGE_BYTES = 10 * 1024 * 1024;
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(8);

    private final HttpClient httpClient;

    public ProductImageProxyService() {
        this(HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build());
    }

    ProductImageProxyService(HttpClient httpClient) {
        this.httpClient = httpClient;
    }

    public ProxyImage fetch(String sourceUrl) {
        URI source = validateSource(sourceUrl);
        HttpRequest request = HttpRequest.newBuilder(source)
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "image/avif,image/webp,image/apng,image/*,*/*;q=0.8")
                .header("Accept-Language", "ko-KR,ko;q=0.9,en;q=0.8")
                .header("Origin", "https://kream.co.kr")
                .header("Referer", "https://kream.co.kr/")
                .header("User-Agent", "Mozilla/5.0 (compatible; ZIK00-ImageProxy/1.0)")
                .GET()
                .build();

        try {
            HttpResponse<InputStream> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "원본 이미지 서버가 이미지를 반환하지 않았습니다."
                );
            }

            MediaType contentType = parseImageContentType(
                    response.headers().firstValue("Content-Type").orElse("")
            );
            long contentLength = response.headers().firstValueAsLong("Content-Length").orElse(-1);
            if (contentLength > MAX_IMAGE_BYTES) {
                throw new ResponseStatusException(
                        HttpStatus.PAYLOAD_TOO_LARGE,
                        "이미지 크기가 허용 범위를 초과했습니다."
                );
            }

            try (InputStream body = response.body()) {
                byte[] bytes = body.readNBytes(MAX_IMAGE_BYTES + 1);
                if (bytes.length > MAX_IMAGE_BYTES) {
                    throw new ResponseStatusException(
                            HttpStatus.PAYLOAD_TOO_LARGE,
                            "이미지 크기가 허용 범위를 초과했습니다."
                    );
                }
                return new ProxyImage(bytes, contentType);
            }
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "이미지 요청이 중단되었습니다.",
                    exception
            );
        } catch (IOException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "원본 이미지를 불러오지 못했습니다.",
                    exception
            );
        }
    }

    URI validateSource(String sourceUrl) {
        final URI source;
        try {
            source = URI.create(sourceUrl);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올바르지 않은 이미지 주소입니다.");
        }

        String host = source.getHost();
        boolean allowed = "https".equalsIgnoreCase(source.getScheme())
                && host != null
                && ALLOWED_HOSTS.contains(host.toLowerCase(Locale.ROOT))
                && source.getUserInfo() == null
                && (source.getPort() == -1 || source.getPort() == 443);
        if (!allowed) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "허용되지 않은 이미지 주소입니다.");
        }
        return source;
    }

    private MediaType parseImageContentType(String value) {
        try {
            MediaType contentType = MediaType.parseMediaType(value);
            if (!"image".equalsIgnoreCase(contentType.getType())) {
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "원본 서버 응답이 이미지가 아닙니다."
                );
            }
            return contentType;
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "원본 이미지 형식을 확인할 수 없습니다."
            );
        }
    }

    public record ProxyImage(byte[] body, MediaType contentType) {
    }
}
