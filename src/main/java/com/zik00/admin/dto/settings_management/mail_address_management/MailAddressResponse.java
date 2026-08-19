package com.zik00.admin.dto.settings_management.mail_address_management;

import java.time.LocalDateTime;

public record MailAddressResponse(
        long id,
        String provider,
        String host,
        int port,
        String username,
        String senderName,
        boolean passwordConfigured,
        boolean active,
        LocalDateTime updatedAt
) {
}
