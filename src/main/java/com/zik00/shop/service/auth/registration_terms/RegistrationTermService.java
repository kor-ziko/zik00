package com.zik00.shop.service.auth.registration_terms;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import com.zik00.admin.repository.settings_management.common.SettingEntryRepository;
import com.zik00.shop.dto.auth.registration_terms.RegistrationTermResponse;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

@Service
@Transactional(readOnly = true)
public class RegistrationTermService {
    private static final String TYPE = "REGISTRATION_TERM";
    private final SettingEntryRepository repository;
    private final ObjectMapper objectMapper;

    public RegistrationTermService(SettingEntryRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public List<RegistrationTermResponse> findActiveTerms() {
        return repository.findByTypeOrderByDisplayOrderAscIdAsc(TYPE).stream()
                .filter(SettingEntry::isActive)
                .map(this::response)
                .toList();
    }

    private RegistrationTermResponse response(SettingEntry entry) {
        Map<String, String> fields = fields(entry.getFieldData());
        return new RegistrationTermResponse(
                entry.getCode(),
                fields.getOrDefault("shortTitle", entry.getName()),
                entry.getName(),
                entry.getContent(),
                fields.getOrDefault("consentLabel", entry.getName() + "에 동의함"),
                !"OPTIONAL".equalsIgnoreCase(fields.get("requirement"))
        );
    }

    private Map<String, String> fields(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<Map<String, String>>() {});
        } catch (JacksonException exception) {
            return Map.of();
        }
    }
}
