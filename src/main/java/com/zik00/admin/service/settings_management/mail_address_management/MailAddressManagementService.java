package com.zik00.admin.service.settings_management.mail_address_management;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import com.zik00.admin.domain.settings_management.mail_address_management.MailSenderAccount;
import com.zik00.admin.dto.settings_management.mail_address_management.MailAddressRequest;
import com.zik00.admin.dto.settings_management.mail_address_management.MailAddressResponse;
import com.zik00.admin.repository.settings_management.mail_address_management.MailAddressRepository;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class MailAddressManagementService {
    private static final String TYPE = "MAIL_ADDRESS";
    private static final String CODE = "PRIMARY";

    private final MailAddressRepository repository;
    private final PiiEncryptionService encryptionService;
    private final ObjectMapper objectMapper;

    public MailAddressManagementService(
            MailAddressRepository repository,
            PiiEncryptionService encryptionService,
            ObjectMapper objectMapper
    ) {
        this.repository = repository;
        this.encryptionService = encryptionService;
        this.objectMapper = objectMapper;
    }

    public Optional<MailAddressResponse> find() {
        return entity().map(this::response);
    }

    public Optional<MailSenderAccount> activeAccount() {
        return entity().filter(SettingEntry::isActive).map(item -> {
            Map<String, String> fields = fields(item);
            return new MailSenderAccount(
                    value(fields, "host"), number(fields, "port", 587), value(fields, "username"),
                    encryptionService.decrypt(value(fields, "password")), value(fields, "senderName")
            );
        });
    }

    @Transactional
    public MailAddressResponse save(MailAddressRequest request) {
        SettingEntry existing = entity().orElse(null);
        Map<String, String> previous = existing == null ? Map.of() : fields(existing);
        String password = clean(request.password());
        if (password.isEmpty()) password = value(previous, "password");
        else password = encryptionService.encrypt(password);
        if (password.isEmpty()) throw bad("메일 비밀번호를 입력해주세요.");

        String host = host(request.provider(), request.host());
        Map<String, String> values = new LinkedHashMap<>();
        values.put("provider", request.provider());
        values.put("host", host);
        values.put("port", Integer.toString(request.port()));
        values.put("username", clean(request.username()));
        values.put("password", password);
        values.put("senderName", clean(request.senderName()));

        if (existing == null) {
            existing = repository.save(new SettingEntry(
                    TYPE, CODE, "회사 발신 메일", null, json(values), 1, request.active()
            ));
        } else {
            existing.update(TYPE, CODE, "회사 발신 메일", null, json(values), 1, request.active());
        }
        return response(existing);
    }

    private Optional<SettingEntry> entity() {
        return repository.findByTypeOrderByIdAsc(TYPE).stream().findFirst();
    }

    private MailAddressResponse response(SettingEntry item) {
        Map<String, String> fields = fields(item);
        return new MailAddressResponse(
                item.getId(), value(fields, "provider"), value(fields, "host"),
                number(fields, "port", 587), value(fields, "username"), value(fields, "senderName"),
                !value(fields, "password").isEmpty(), item.isActive(), item.getUpdatedAt()
        );
    }

    private String host(String provider, String customHost) {
        return switch (provider) {
            case "NAVER" -> "smtp.naver.com";
            case "GMAIL" -> "smtp.gmail.com";
            case "CUSTOM" -> {
                String host = clean(customHost);
                if (host.isEmpty()) throw bad("SMTP 서버 주소를 입력해주세요.");
                yield host;
            }
            default -> throw bad("메일 서비스를 선택해주세요.");
        };
    }

    private Map<String, String> fields(SettingEntry item) {
        try {
            Map<?, ?> parsed = objectMapper.readValue(item.getFieldData(), Map.class);
            Map<String, String> result = new LinkedHashMap<>();
            parsed.forEach((key, value) -> result.put(String.valueOf(key), String.valueOf(value)));
            return result;
        } catch (JacksonException exception) {
            throw bad("메일주소 설정을 읽을 수 없습니다.");
        }
    }

    private String json(Map<String, String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JacksonException exception) {
            throw bad("메일주소 설정을 저장할 수 없습니다.");
        }
    }

    private int number(Map<String, String> fields, String key, int fallback) {
        try {
            return Integer.parseInt(value(fields, key));
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    private String value(Map<String, String> fields, String key) {
        return fields.getOrDefault(key, "");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private ResponseStatusException bad(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }
}
