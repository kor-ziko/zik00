package com.zik00.admin.auth;

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
            HttpSession httpSession
    ) {
        return AdminSessionResponse.from(adminAuthService.login(request, httpSession));
    }

    @GetMapping("/me")
    public AdminSessionResponse me(HttpSession httpSession) {
        return AdminSessionResponse.from(adminAuthService.current(httpSession));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(HttpSession httpSession) {
        adminAuthService.logout(httpSession);
    }
}
