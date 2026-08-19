package com.zik00.admin.dto.settings_management.mail_management;

public record MailDeliveryStatusResponse(
        boolean enabled,
        boolean configured,
        String message
) {
}
