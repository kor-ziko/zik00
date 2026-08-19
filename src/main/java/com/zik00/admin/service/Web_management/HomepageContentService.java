package com.zik00.admin.service.Web_management;

import com.zik00.admin.domain.Web_management.HomepageContent;
import com.zik00.admin.dto.Web_management.HomepageContentRequest;
import com.zik00.admin.dto.Web_management.HomepageContentResponse;
import com.zik00.admin.repository.Web_management.HomepageContentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.time.LocalDateTime;
import java.net.URI;
import java.util.Set;

@Service
@Transactional(readOnly = true)
public class HomepageContentService {
    private static final Set<String> IMAGE_TYPES = Set.of("MAIN_BANNER", "OTHER_BANNER", "POPUP", "RECOMMENDED_SITE");
    private final HomepageContentRepository repository;

    public HomepageContentService(HomepageContentRepository repository) {
        this.repository = repository;
    }

    public List<HomepageContentResponse> findByType(String type) {
        List<HomepageContent> items = "PRECAUTION".equals(type)
                ? repository.findByContentTypeOrderByApplicationTypeAscDisplayOrderAscIdAsc(type)
                : repository.findByContentTypeOrderByDisplayOrderAscIdAsc(type);
        return items.stream()
                .map(HomepageContentResponse::from).toList();
    }

    public List<HomepageContentResponse> findActive() {
        LocalDateTime now = LocalDateTime.now();
        return repository.findByActiveTrueOrderByContentTypeAscDisplayOrderAscIdAsc().stream()
                .filter(item -> item.getStartsAt() == null || !item.getStartsAt().isAfter(now))
                .filter(item -> item.getEndsAt() == null || !item.getEndsAt().isBefore(now))
                .map(HomepageContentResponse::from).toList();
    }

    @Transactional
    public HomepageContentResponse create(String type, HomepageContentRequest request) {
        validateContent(type, request);
        String applicationType = applicationType(type, request.applicationType());
        int displayOrder = displayOrderForCreate(type, applicationType, request.displayOrder());
        return HomepageContentResponse.from(repository.save(new HomepageContent(
                type, clean(request.title()), nullable(request.subtitle()), nullable(request.content()),
                nullable(request.imageUrl()), nullable(request.linkUrl()), nullable(request.linkLabel()),
                applicationType, displayOrder, request.active(), request.startsAt(), request.endsAt())));
    }

    @Transactional
    public HomepageContentResponse update(String type, long id, HomepageContentRequest request) {
        validateContent(type, request);
        HomepageContent item = find(type, id);
        String applicationType = applicationType(type, request.applicationType());
        int displayOrder = displayOrderForUpdate(type, item, applicationType, request.displayOrder());
        item.update(type, clean(request.title()), nullable(request.subtitle()), nullable(request.content()),
                nullable(request.imageUrl()), nullable(request.linkUrl()), nullable(request.linkLabel()),
                applicationType, displayOrder, request.active(), request.startsAt(), request.endsAt());
        return HomepageContentResponse.from(item);
    }

    @Transactional
    public void delete(String type, long id) {
        repository.delete(find(type, id));
    }

    private HomepageContent find(String type, long id) {
        HomepageContent item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다."));
        if (!item.getContentType().equals(type)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "콘텐츠를 찾을 수 없습니다.");
        }
        return item;
    }

    private void validatePeriod(HomepageContentRequest request) {
        if (request.startsAt() != null && request.endsAt() != null && request.endsAt().isBefore(request.startsAt())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "종료일은 시작일 이후여야 합니다.");
        }
    }

    private void validateContent(String type, HomepageContentRequest request) {
        validatePeriod(request);
        if (IMAGE_TYPES.contains(type) && nullable(request.imageUrl()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "이미지 파일을 등록해주세요.");
        }
        if (IMAGE_TYPES.contains(type) && nullable(request.linkUrl()) == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연결할 페이지 또는 URL을 입력해주세요.");
        }
        String link = nullable(request.linkUrl());
        if (link != null && !isAllowedLink(link)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "연결 URL은 홈페이지 경로 또는 http(s) 주소만 사용할 수 있습니다.");
        }
    }

    private boolean isAllowedLink(String value) {
        if (value.startsWith("/") || value.startsWith("#")) return true;
        try {
            String scheme = URI.create(value).getScheme();
            return "http".equalsIgnoreCase(scheme) || "https".equalsIgnoreCase(scheme);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    private String clean(String value) { return value.trim(); }
    private String nullable(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String applicationType(String contentType, String value) {
        if (!"PRECAUTION".equals(contentType)) return null;
        if (!"DELIVERY_AGENCY".equals(value) && !"PURCHASE_AGENCY".equals(value)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "적용구분을 선택해주세요.");
        }
        return value;
    }

    private int displayOrderForCreate(String contentType, String applicationType, int requestedOrder) {
        if (!"PRECAUTION".equals(contentType)) return requestedOrder;
        return repository.findMaxDisplayOrder(contentType, applicationType) + 1;
    }

    private int displayOrderForUpdate(String contentType, HomepageContent item,
                                      String applicationType, int requestedOrder) {
        if (!"PRECAUTION".equals(contentType)) return requestedOrder;
        if (!applicationType.equals(item.getApplicationType())) {
            return repository.findMaxDisplayOrder(contentType, applicationType) + 1;
        }
        return requestedOrder;
    }
}
