package com.zik00.admin.service.settings_management.mail_management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import com.zik00.admin.dto.settings_management.mail_management.MailTemplateRequest;
import com.zik00.admin.repository.settings_management.mail_management.MailTemplateRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class MailTemplateServiceTest {
    @Test
    void selectingNewDefaultClearsPreviousDefaultOfSameType() {
        MailTemplateRepository repository = mock(MailTemplateRepository.class);
        SettingEntry previous = new SettingEntry(
                "MAIL_TEMPLATE", "OLD_WELCOME", "기존 환영 메일", "기존 본문",
                "{\"templateType\":\"SIGNUP\",\"subject\":\"기존 제목\",\"senderName\":\"ZIK:00\",\"replyTo\":\"old@example.com\",\"defaultTemplate\":\"true\"}",
                1, true
        );
        ReflectionTestUtils.setField(previous, "id", 1L);
        when(repository.findByTypeInOrderByDisplayOrderAscIdAsc(any())).thenReturn(List.of(previous));
        when(repository.save(any(SettingEntry.class))).thenAnswer(invocation -> {
            SettingEntry saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 2L);
            return saved;
        });
        MailTemplateService service = new MailTemplateService(repository, new ObjectMapper());
        MailTemplateRequest request = new MailTemplateRequest(
                "NEW_WELCOME", "새 환영 메일", "SIGNUP", "새 제목", "ZIK:00",
                "reply@example.com", "새 본문", true, 2, true
        );

        var created = service.create(request);

        assertThat(created.defaultTemplate()).isTrue();
        assertThat(previous.getFieldData()).contains("\"defaultTemplate\":\"false\"");
        assertThat(previous.getType()).isEqualTo("MAIL_TEMPLATE");
    }
}
