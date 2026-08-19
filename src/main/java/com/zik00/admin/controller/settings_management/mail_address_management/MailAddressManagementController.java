package com.zik00.admin.controller.settings_management.mail_address_management;

import com.zik00.admin.dto.settings_management.mail_address_management.MailAddressRequest;
import com.zik00.admin.dto.settings_management.mail_address_management.MailAddressResponse;
import com.zik00.admin.dto.settings_management.mail_management.MailDeliveryStatusResponse;
import com.zik00.admin.dto.settings_management.mail_management.MailTestRequest;
import com.zik00.admin.service.settings_management.mail_address_management.ConfiguredMailSenderService;
import com.zik00.admin.service.settings_management.mail_address_management.MailAddressManagementService;
import jakarta.mail.AuthenticationFailedException;
import jakarta.mail.SendFailedException;
import jakarta.validation.Valid;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings-management/mail-address")
public class MailAddressManagementController {
    private static final Logger log = LoggerFactory.getLogger(MailAddressManagementController.class);
    private final MailAddressManagementService managementService;
    private final ConfiguredMailSenderService mailSenderService;

    public MailAddressManagementController(
            MailAddressManagementService managementService,
            ConfiguredMailSenderService mailSenderService
    ) {
        this.managementService = managementService;
        this.mailSenderService = mailSenderService;
    }

    @GetMapping
    public ResponseEntity<MailAddressResponse> find() {
        return managementService.find().map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.noContent().build());
    }

    @PutMapping
    public MailAddressResponse save(@Valid @RequestBody MailAddressRequest request) {
        return managementService.save(request);
    }

    @GetMapping("/status")
    public MailDeliveryStatusResponse status() {
        return mailSenderService.status();
    }

    @PostMapping("/test")
    public ResponseEntity<?> test(@Valid @RequestBody MailTestRequest request) {
        try {
            mailSenderService.testConnection();
            mailSenderService.send("ZIK:00", request.recipient(), "ZIK:00 메일 발송 테스트",
                    "<p>회사 발신 메일 설정이 정상적으로 연결되었습니다.</p>");
            return ResponseEntity.noContent().build();
        } catch (Exception exception) {
            log.warn("Company mail connection test failed. provider={}", provider(), exception);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(new MailTestErrorResponse(List.of(failureMessage(exception))));
        }
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<MailTestErrorResponse> validationFailure(MethodArgumentNotValidException exception) {
        List<String> messages = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null
                        ? "입력한 내용을 확인해주세요."
                        : error.getDefaultMessage())
                .distinct()
                .toList();
        return ResponseEntity.badRequest().body(new MailTestErrorResponse(messages));
    }

    private String provider() {
        return managementService.find().map(MailAddressResponse::provider).orElse("UNKNOWN");
    }

    private String failureMessage(Exception exception) {
        if (hasCause(exception, AuthenticationFailedException.class)) {
            return providerAuthenticationFailureMessage();
        }
        if (hasCause(exception, SendFailedException.class)) {
            return "메일 서버가 받는 이메일 주소를 거부했습니다. 주소가 정확한지 확인해주세요.";
        }
        if (hasCause(exception, SocketTimeoutException.class)) {
            return "메일 서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.";
        }
        if (hasCause(exception, ConnectException.class)) {
            return "메일 서버에 연결할 수 없습니다. 네트워크와 SMTP 설정을 확인해주세요.";
        }
        return providerConnectionFailureMessage();
    }

    private String providerAuthenticationFailureMessage() {
        if ("NAVER".equals(provider())) {
            return "네이버 메일 인증에 실패했습니다. IMAP/SMTP 사용 설정과 애플리케이션 비밀번호를 확인해주세요.";
        }
        if ("GMAIL".equals(provider())) {
            return "Gmail 인증에 실패했습니다. 2단계 인증용 앱 비밀번호와 계정 설정을 확인해주세요.";
        }
        return "메일 계정 인증에 실패했습니다. 메일 주소와 비밀번호를 확인해주세요.";
    }

    private String providerConnectionFailureMessage() {
        if ("NAVER".equals(provider())) {
            return "네이버 메일 발송에 실패했습니다. 받는 주소와 네이버 메일 설정을 확인해주세요.";
        }
        if ("GMAIL".equals(provider())) {
            return "Gmail 발송에 실패했습니다. 받는 주소와 Gmail 계정 설정을 확인해주세요.";
        }
        return "메일 연결에 실패했습니다. 메일 주소, 비밀번호, SMTP 서버 설정을 확인해주세요.";
    }

    private boolean hasCause(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) return true;
            current = current.getCause();
        }
        return false;
    }

    private record MailTestErrorResponse(List<String> messages) {
    }
}
