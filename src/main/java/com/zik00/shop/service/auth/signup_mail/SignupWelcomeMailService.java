package com.zik00.shop.service.auth.signup_mail;

import com.zik00.admin.domain.settings_management.mail_management.MailTemplateType;
import com.zik00.admin.dto.settings_management.mail_management.MailTemplateResponse;
import com.zik00.admin.service.settings_management.mail_management.MailTemplateService;
import com.zik00.admin.service.settings_management.mail_management.SignupMailLayoutRenderer;
import com.zik00.admin.service.settings_management.mail_address_management.ConfiguredMailSenderService;
import com.zik00.shop.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class SignupWelcomeMailService {
    private static final Logger log = LoggerFactory.getLogger(SignupWelcomeMailService.class);
    private final ConfiguredMailSenderService mailSenderService;
    private final MailTemplateService mailTemplateService;
    private final SignupMailLayoutRenderer layoutRenderer;

    public SignupWelcomeMailService(
            ConfiguredMailSenderService mailSenderService,
            MailTemplateService mailTemplateService,
            SignupMailLayoutRenderer layoutRenderer
    ) {
        this.mailSenderService = mailSenderService;
        this.mailTemplateService = mailTemplateService;
        this.layoutRenderer = layoutRenderer;
    }

    public void send(User user) {
        if (normalize(user.getEmail()).isEmpty()) {
            log.warn("Signup mail skipped because member email is empty. accessId={}", user.getAccessId());
            return;
        }
        try {
            if (!mailSenderService.status().configured()) {
                log.warn("Signup mail skipped because company mail address is not configured.");
                return;
            }
            MailTemplateResponse template = mailTemplateService.findDefaultActive(MailTemplateType.SIGNUP).orElse(null);
            if (template == null) {
                log.warn("Signup mail skipped because no active signup mail template exists.");
                return;
            }
            String subject = replaceMemberValues(template.subject(), user);
            String contentFragment = replaceMemberValues(template.content(), user);
            String content = layoutRenderer.render(
                    contentFragment,
                    template.senderName(),
                    user.getUserId(),
                    "ZK%06d".formatted(user.getMemberId()),
                    user.getMobilePhone(),
                    template.replyTo()
            );
            mailSenderService.send(template.senderName(), user.getEmail(), subject, content);
        } catch (Exception exception) {
            log.error("Failed to send signup mail. accessId={}", user.getAccessId(), exception);
        }
    }

    private String replaceMemberValues(String value, User user) {
        return normalize(value)
                .replace("${name}", normalize(user.getName()))
                .replace("${nickname}", normalize(user.getNickname()))
                .replace("${email}", normalize(user.getEmail()))
                .replace("${loginId}", normalize(user.getUserId()))
                .replace("${mobilePhone}", normalize(user.getMobilePhone()));
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
