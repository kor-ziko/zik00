package com.zik00.admin.dto.settings_management.mail_management;

import java.time.LocalDateTime;

public record MailTemplateResponse(
        Long id,
        String code,
        String name,
        String templateType,
        String subject,
        String senderName,
        String replyTo,
        String content,
        boolean defaultTemplate,
        int displayOrder,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
