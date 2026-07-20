package com.zik00.shop.controller;

import java.util.List;

import com.zik00.shop.dto.mypage.JapanPostalCodeResponse;
import com.zik00.shop.service.JapanPostalCodeSearchService;
import com.zik00.shop.service.PostalCodeRequestRateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/japan-postal-codes")
public class JapanPostalCodeController {
    private final JapanPostalCodeSearchService japanPostalCodeSearchService;
    private final PostalCodeRequestRateLimiter requestRateLimiter;

    public JapanPostalCodeController(
            JapanPostalCodeSearchService japanPostalCodeSearchService,
            PostalCodeRequestRateLimiter requestRateLimiter
    ) {
        this.japanPostalCodeSearchService = japanPostalCodeSearchService;
        this.requestRateLimiter = requestRateLimiter;
    }

    @GetMapping
    public List<JapanPostalCodeResponse> findByPostalCode(
            @RequestParam String postalCode,
            HttpServletRequest request
    ) {
        requestRateLimiter.check(request);
        return japanPostalCodeSearchService.findByPostalCode(postalCode);
    }
}
