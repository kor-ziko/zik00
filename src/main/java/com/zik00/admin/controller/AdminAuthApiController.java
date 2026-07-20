package com.zik00.admin.controller;

import com.zik00.admin.dto.AdminLoginRequest;
import com.zik00.admin.dto.AdminSessionResponse;
import com.zik00.admin.service.AdminAuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthApiController {
    private final AdminAuthService adminAuthService;

    public AdminAuthApiController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @PostMapping("/login")
    public AdminSessionResponse login(
            @Valid @RequestBody AdminLoginRequest request,
            HttpServletRequest httpRequest
    ) {
        HttpSession httpSession = httpRequest.getSession();
        AdminSessionResponse response = AdminSessionResponse.from(adminAuthService.login(request, httpSession));
        httpRequest.changeSessionId();
        return response;
    }

    @GetMapping("/me")
    public AdminSessionResponse me(HttpServletRequest request) {
        return AdminSessionResponse.from(adminAuthService.current(request.getSession(false)));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpServletRequest request) {
        adminAuthService.logout(request.getSession(false));
    }
}
