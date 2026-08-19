package com.zik00.admin.dto.settings_management.mail_management;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record MailTemplateRequest(
        @NotBlank @Size(max = 100)
        @Pattern(regexp = "^[A-Za-z0-9_-]+$", message = "메일 코드는 영문, 숫자, 밑줄, 하이픈만 사용할 수 있습니다.")
        String code,
        @NotBlank @Size(max = 200) String name,
        @NotBlank @Pattern(regexp = "SIGNUP|MARKETING|NOTICE|CUSTOM", message = "메일 종류가 올바르지 않습니다.")
        String templateType,
        @NotBlank @Size(max = 300) String subject,
        @NotBlank @Size(max = 100) String senderName,
        @Email @Size(max = 255) String replyTo,
        @NotBlank @Size(max = 50_000) String content,
        boolean defaultTemplate,
        @Min(0) int displayOrder,
        boolean active
) {
}
