package com.zik00.admin.dto.settings_management.mail_management;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MailTestRequest(
        @NotBlank(message = "테스트 받을 이메일 주소를 입력해주세요.")
        @Email(message = "테스트 받을 이메일 주소를 정확히 입력해주세요.")
        @Size(max = 255, message = "테스트 받을 이메일 주소는 255자 이하로 입력해주세요.")
        String recipient
) {
}
