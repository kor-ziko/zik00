package com.zik00.admin.service;

import com.zik00.admin.domain.AdminUser;
import com.zik00.admin.dto.AdminLoginRequest;
import com.zik00.admin.dto.AdminSession;
import com.zik00.admin.repository.AdminUserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminAuthService {
    public static final String SESSION_ATTRIBUTE = "ADMIN_SESSION";

    private final AdminUserRepository adminUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminAuthService(
            AdminUserRepository adminUserRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.adminUserRepository = adminUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AdminSession login(AdminLoginRequest request, HttpSession httpSession) {
        httpSession.removeAttribute(SESSION_ATTRIBUTE);

        AdminUser adminUser = adminUserRepository.findByLoginId(normalize(request.loginId()))
                .filter(AdminUser::isActive)
                .filter(user -> passwordEncoder.matches(request.password(), user.getPasswordHash()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 올바르지 않습니다."));

        AdminSession adminSession = new AdminSession(
                adminUser.getAdminId(),
                adminUser.getLoginId(),
                adminUser.getName()
        );
        httpSession.setAttribute(SESSION_ATTRIBUTE, adminSession);
        return adminSession;
    }

    public AdminSession current(HttpSession httpSession) {
        Object session = httpSession == null ? null : httpSession.getAttribute(SESSION_ATTRIBUTE);
        if (session instanceof AdminSession adminSession) {
            return adminSession;
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "관리자 로그인이 필요합니다.");
    }

    public void logout(HttpSession httpSession) {
        if (httpSession != null) {
            httpSession.invalidate();
        }
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
