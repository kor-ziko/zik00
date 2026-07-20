package com.zik00.shop.controller.auth;

import com.zik00.shop.service.auth.JwtSessionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LogoutController {
    private final JwtSessionService jwtSessionService;

    public LogoutController(JwtSessionService jwtSessionService) {
        this.jwtSessionService = jwtSessionService;
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        jwtSessionService.logout(request, response);
        return ResponseEntity.noContent().build();
    }
}
