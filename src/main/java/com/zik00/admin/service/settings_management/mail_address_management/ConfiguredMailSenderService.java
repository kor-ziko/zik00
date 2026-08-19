package com.zik00.admin.service.settings_management.mail_address_management;

import com.zik00.admin.domain.settings_management.mail_address_management.MailSenderAccount;
import com.zik00.admin.dto.settings_management.mail_management.MailDeliveryStatusResponse;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import org.jsoup.Jsoup;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ConfiguredMailSenderService {
    private final MailAddressManagementService managementService;

    public ConfiguredMailSenderService(MailAddressManagementService managementService) {
        this.managementService = managementService;
    }

    public MailDeliveryStatusResponse status() {
        boolean configured = managementService.activeAccount().isPresent();
        String message = configured
                ? "회사 발신 메일이 연결되어 있습니다."
                : "메일주소관리에서 회사 발신 메일을 등록해주세요.";
        return new MailDeliveryStatusResponse(configured, configured, message);
    }

    public void testConnection() throws Exception {
        sender(requiredAccount()).testConnection();
    }

    public void send(String senderName, String recipient, String subject, String html) throws Exception {
        MailSenderAccount account = requiredAccount();
        JavaMailSenderImpl sender = sender(account);
        MimeMessage message = sender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(message, StandardCharsets.UTF_8.name());
        helper.setFrom(account.username(), clean(senderName).isEmpty() ? account.senderName() : clean(senderName));
        helper.setTo(clean(recipient));
        helper.setSubject(clean(subject));
        helper.setReplyTo(account.username());
        helper.setText(withoutImages(html), true);
        sender.send(message);
    }

    private MailSenderAccount requiredAccount() {
        return managementService.activeAccount()
                .orElseThrow(() -> new IllegalStateException("회사 발신 메일이 설정되지 않았습니다."));
    }

    private JavaMailSenderImpl sender(MailSenderAccount account) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(account.host());
        sender.setPort(account.port());
        sender.setUsername(account.username());
        sender.setPassword(account.password());
        sender.setDefaultEncoding(StandardCharsets.UTF_8.name());
        Properties properties = sender.getJavaMailProperties();
        properties.put("mail.smtp.auth", "true");
        properties.put("mail.smtp.starttls.enable", "true");
        properties.put("mail.smtp.connectiontimeout", "10000");
        properties.put("mail.smtp.timeout", "10000");
        properties.put("mail.smtp.writetimeout", "10000");
        return sender;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String withoutImages(String html) {
        var document = Jsoup.parse(html == null ? "" : html);
        document.select("img,picture,source").remove();
        return document.outerHtml();
    }
}
