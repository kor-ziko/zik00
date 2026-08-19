package com.zik00.admin.controller.settings_management.admin_management;

import com.zik00.admin.dto.AdminSession;
import com.zik00.admin.dto.settings_management.admin_management.AdminManagementRequest;
import com.zik00.admin.dto.settings_management.admin_management.AdminManagementResponse;
import com.zik00.admin.service.AdminAuthService;
import com.zik00.admin.service.settings_management.admin_management.AdminManagementService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/settings-management/admins")
public class AdminManagementController {
    private final AdminManagementService service;
    private final AdminAuthService authService;

    public AdminManagementController(AdminManagementService service, AdminAuthService authService) {
        this.service = service;
        this.authService = authService;
    }

    @GetMapping public List<AdminManagementResponse> findAll() { return service.findAll(); }
    @PostMapping public AdminManagementResponse create(@Valid @RequestBody AdminManagementRequest request) { return service.create(request); }
    @PutMapping("/{id}") public AdminManagementResponse update(@PathVariable long id, @Valid @RequestBody AdminManagementRequest request) { return service.update(id, request); }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable long id, Authentication authentication) {
        AdminSession current = authService.current(authentication);
        service.delete(id, current.adminId());
        return ResponseEntity.noContent().build();
    }
}
