package com.zik00.admin.controller.settings_management.mail_management;

import com.zik00.admin.dto.settings_management.mail_management.MailTemplateRequest;
import com.zik00.admin.dto.settings_management.mail_management.MailTemplateResponse;
import com.zik00.admin.dto.settings_management.mail_management.MailDeliveryStatusResponse;
import com.zik00.admin.dto.settings_management.mail_management.MailTestRequest;
import com.zik00.admin.service.settings_management.mail_management.AdminMailTestService;
import com.zik00.admin.service.settings_management.mail_management.MailTemplateService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/settings-management/mail-templates")
public class MailTemplateController {
    private final MailTemplateService service;
    private final AdminMailTestService mailTestService;

    public MailTemplateController(MailTemplateService service, AdminMailTestService mailTestService) {
        this.service = service;
        this.mailTestService = mailTestService;
    }

    @GetMapping
    public List<MailTemplateResponse> findAll() { return service.findAll(); }

    @GetMapping("/delivery-status")
    public MailDeliveryStatusResponse deliveryStatus() {
        return mailTestService.status();
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Void> sendTest(@PathVariable long id, @Valid @RequestBody MailTestRequest request) {
        mailTestService.send(id, request.recipient());
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MailTemplateResponse create(@Valid @RequestBody MailTemplateRequest request) {
        return service.create(request);
    }

    @PutMapping("/{id}")
    public MailTemplateResponse update(@PathVariable long id, @Valid @RequestBody MailTemplateRequest request) {
        return service.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
