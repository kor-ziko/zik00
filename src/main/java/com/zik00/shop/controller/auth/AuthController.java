package com.zik00.shop.controller.auth;

import com.zik00.shop.config.WebClientOrigins;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AuthController {
    private final String frontendBaseUrl;

    public AuthController(WebClientOrigins webClientOrigins) {
        this.frontendBaseUrl = webClientOrigins.clientBaseUrl();
    }

    @GetMapping("/login")
    public String login() {
        return "redirect:" + frontendBaseUrl + "/login";
    }

    @GetMapping("/signup/detail")
    public String detail() {
        return "redirect:" + frontendBaseUrl + "/login/detail";
    }
}
