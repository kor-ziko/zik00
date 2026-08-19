package com.zik00.admin.domain.settings_management.mail_address_management;

public record MailSenderAccount(
        String host,
        int port,
        String username,
        String password,
        String senderName
) {
}
