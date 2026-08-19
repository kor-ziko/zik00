package com.zik00.admin.dto.settings_management.mail_address_management;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MailAddressRequest(
        @NotBlank @Pattern(regexp = "NAVER|GMAIL|CUSTOM", message = "메일 서비스를 선택해주세요.")
        String provider,
        @Size(max = 255) String host,
        @Min(1) @Max(65535) int port,
        @NotBlank @Email @Size(max = 255) String username,
        @Size(max = 500) String password,
        @NotBlank @Size(max = 100) String senderName,
        boolean active
) {
}
