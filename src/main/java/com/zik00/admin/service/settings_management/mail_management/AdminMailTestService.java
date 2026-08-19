package com.zik00.admin.service.settings_management.mail_management;

import com.zik00.admin.dto.settings_management.mail_management.MailDeliveryStatusResponse;
import com.zik00.admin.dto.settings_management.mail_management.MailTemplateResponse;
import com.zik00.admin.service.settings_management.mail_address_management.ConfiguredMailSenderService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminMailTestService {
    private final ConfiguredMailSenderService mailSenderService;
    private final MailTemplateService mailTemplateService;
    private final SignupMailLayoutRenderer signupMailLayoutRenderer;

    public AdminMailTestService(
            ConfiguredMailSenderService mailSenderService,
            MailTemplateService mailTemplateService,
            SignupMailLayoutRenderer signupMailLayoutRenderer
    ) {
        this.mailSenderService = mailSenderService;
        this.mailTemplateService = mailTemplateService;
        this.signupMailLayoutRenderer = signupMailLayoutRenderer;
    }

    public MailDeliveryStatusResponse status() {
        return mailSenderService.status();
    }

    public void send(long templateId, String recipient) {
        MailDeliveryStatusResponse status = status();
        if (!status.enabled() || !status.configured()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, status.message());
        }

        MailTemplateResponse template = mailTemplateService.findById(templateId);
        try {
            String content = replaceSampleValues(template.content());
            if ("SIGNUP".equals(template.templateType())) {
                content = signupMailLayoutRenderer.render(
                        content, template.senderName(), "sample_member", "ZK000001",
                        "010-1234-5678", template.replyTo()
                );
            }
            mailSenderService.send(
                    template.senderName(), clean(recipient),
                    replaceSampleValues(template.subject()), content
            );
        } catch (Exception exception) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "테스트 메일을 보내지 못했습니다. 공용 발송 계정의 SMTP 설정을 확인해주세요.",
                    exception
            );
        }
    }

    private String replaceSampleValues(String value) {
        return clean(value)
                .replace("${name}", "테스트 회원")
                .replace("${nickname}", "테스트")
                .replace("${email}", "member@example.com")
                .replace("${loginId}", "sample_member")
                .replace("${mobilePhone}", "010-1234-5678");
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
