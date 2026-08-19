package com.zik00.admin.service.settings_management.mail_management;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import com.zik00.admin.domain.settings_management.mail_management.MailTemplateType;
import com.zik00.admin.dto.settings_management.mail_management.MailTemplateRequest;
import com.zik00.admin.dto.settings_management.mail_management.MailTemplateResponse;
import com.zik00.admin.repository.settings_management.mail_management.MailTemplateRepository;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.jsoup.Jsoup;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class MailTemplateService {
    private static final String TYPE = "MAIL_TEMPLATE";
    private static final String LEGACY_TYPE = "SIGNUP_MAIL";
    private static final List<String> TYPES = List.of(TYPE, LEGACY_TYPE);

    private final MailTemplateRepository repository;
    private final ObjectMapper objectMapper;

    public MailTemplateService(MailTemplateRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<MailTemplateResponse> findAll() {
        return repository.findByTypeInOrderByDisplayOrderAscIdAsc(TYPES).stream()
                .map(this::response)
                .sorted((left, right) -> Integer.compare(left.displayOrder(), right.displayOrder()))
                .toList();
    }

    public Optional<MailTemplateResponse> findDefaultActive(MailTemplateType templateType) {
        List<MailTemplateResponse> candidates = findAll().stream()
                .filter(MailTemplateResponse::active)
                .filter(item -> templateType.name().equals(item.templateType()))
                .toList();
        return candidates.stream().filter(MailTemplateResponse::defaultTemplate).findFirst()
                .or(() -> candidates.stream().findFirst());
    }

    public MailTemplateResponse findById(long id) {
        return response(find(id));
    }

    @Transactional
    public MailTemplateResponse create(MailTemplateRequest request) {
        validateUniqueCode(request.code(), 0L);
        MailTemplateType templateType = MailTemplateType.valueOf(request.templateType());
        if (request.defaultTemplate()) clearDefault(templateType, 0L);
        SettingEntry saved = repository.save(new SettingEntry(
                TYPE, clean(request.code()), clean(request.name()), cleanContent(request.content()),
                fields(request), request.displayOrder(), request.active()
        ));
        return response(saved);
    }

    @Transactional
    public MailTemplateResponse update(long id, MailTemplateRequest request) {
        SettingEntry item = find(id);
        validateUniqueCode(request.code(), id);
        MailTemplateType templateType = MailTemplateType.valueOf(request.templateType());
        if (request.defaultTemplate()) clearDefault(templateType, id);
        item.update(
                TYPE, clean(request.code()), clean(request.name()), cleanContent(request.content()),
                fields(request), request.displayOrder(), request.active()
        );
        return response(item);
    }

    @Transactional
    public void delete(long id) {
        repository.delete(find(id));
    }

    private SettingEntry find(long id) {
        SettingEntry item = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "메일 템플릿을 찾을 수 없습니다."));
        if (!TYPES.contains(item.getType())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "메일 템플릿을 찾을 수 없습니다.");
        }
        return item;
    }

    private void validateUniqueCode(String code, long id) {
        boolean duplicate = repository.findByTypeInOrderByDisplayOrderAscIdAsc(TYPES).stream()
                .anyMatch(item -> item.getId() != id && item.getCode().equalsIgnoreCase(clean(code)));
        if (duplicate) throw bad("이미 사용 중인 메일 코드입니다.");
    }

    private void clearDefault(MailTemplateType templateType, long exceptId) {
        repository.findByTypeInOrderByDisplayOrderAscIdAsc(TYPES).stream()
                .filter(item -> item.getId() != exceptId)
                .filter(item -> templateType.name().equals(templateType(item)))
                .filter(item -> booleanField(item, "defaultTemplate"))
                .forEach(item -> {
                    Map<String, String> values = values(item);
                    values.put("defaultTemplate", "false");
                    item.update(TYPE, item.getCode(), item.getName(), item.getContent(), json(values),
                            item.getDisplayOrder(), item.isActive());
                });
    }

    private MailTemplateResponse response(SettingEntry item) {
        Map<String, String> values = values(item);
        return new MailTemplateResponse(
                item.getId(), item.getCode(), item.getName(), templateType(item),
                value(values, "subject"), value(values, "senderName"), value(values, "replyTo"),
                item.getContent() == null ? "" : item.getContent(),
                booleanField(values, "defaultTemplate"), item.getDisplayOrder(), item.isActive(),
                item.getCreatedAt(), item.getUpdatedAt()
        );
    }

    private String fields(MailTemplateRequest request) {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("templateType", request.templateType());
        values.put("subject", clean(request.subject()));
        values.put("senderName", clean(request.senderName()));
        values.put("replyTo", clean(request.replyTo()));
        values.put("defaultTemplate", Boolean.toString(request.defaultTemplate()));
        return json(values);
    }

    private String templateType(SettingEntry item) {
        String value = value(values(item), "templateType");
        return value.isBlank() && LEGACY_TYPE.equals(item.getType()) ? MailTemplateType.SIGNUP.name() : value;
    }

    private boolean booleanField(SettingEntry item, String key) {
        return booleanField(values(item), key);
    }

    private boolean booleanField(Map<String, String> values, String key) {
        return Boolean.parseBoolean(value(values, key));
    }

    private String value(Map<String, String> values, String key) {
        return values.getOrDefault(key, "");
    }

    private Map<String, String> values(SettingEntry item) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(item.getFieldData(), Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            parsed.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
            return result;
        } catch (JacksonException exception) {
            throw bad("메일 템플릿 설정을 읽을 수 없습니다.");
        }
    }

    private String json(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JacksonException exception) {
            throw bad("메일 템플릿 설정을 저장할 수 없습니다.");
        }
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String cleanContent(String value) {
        var document = Jsoup.parseBodyFragment(clean(value));
        document.select("img,picture,source").remove();
        return document.body().html().trim();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
