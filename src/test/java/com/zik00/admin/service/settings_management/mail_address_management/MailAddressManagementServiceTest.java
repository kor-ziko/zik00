package com.zik00.admin.service.settings_management.mail_address_management;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import com.zik00.admin.dto.settings_management.mail_address_management.MailAddressRequest;
import com.zik00.admin.repository.settings_management.mail_address_management.MailAddressRepository;
import com.zik00.shop.service.security.PiiEncryptionService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

class MailAddressManagementServiceTest {
    @Test
    void keepsEncryptedPasswordOnUpdateAndDecryptsOnlyForSending() {
        MailAddressRepository repository = mock(MailAddressRepository.class);
        PiiEncryptionService encryptionService = mock(PiiEncryptionService.class);
        SettingEntry entry = new SettingEntry(
                "MAIL_ADDRESS", "PRIMARY", "회사 발신 메일", null,
                "{\"provider\":\"NAVER\",\"host\":\"smtp.naver.com\",\"port\":\"587\","
                        + "\"username\":\"company@example.com\",\"password\":\"enc:v1:stored\","
                        + "\"senderName\":\"ZIK:00\",\"replyTo\":\"support@example.com\"}",
                1, true
        );
        ReflectionTestUtils.setField(entry, "id", 1L);
        when(repository.findByTypeOrderByIdAsc("MAIL_ADDRESS")).thenReturn(List.of(entry));
        when(encryptionService.decrypt("enc:v1:stored")).thenReturn("smtp-app-password");
        MailAddressManagementService service = new MailAddressManagementService(
                repository, encryptionService, new ObjectMapper()
        );

        var response = service.save(new MailAddressRequest(
                "NAVER", "", 587, "company@example.com", "", "회사 메일", true
        ));

        assertThat(response.passwordConfigured()).isTrue();
        assertThat(entry.getFieldData()).contains("enc:v1:stored").doesNotContain("smtp-app-password");
        assertThat(service.activeAccount()).get().extracting(account -> account.password())
                .isEqualTo("smtp-app-password");
        verify(encryptionService, never()).encrypt(org.mockito.ArgumentMatchers.anyString());
    }
}
