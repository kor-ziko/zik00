package com.zik00.shop.controller;

import java.util.List;

import com.zik00.shop.dto.JapanPostalCodeResponse;
import com.zik00.shop.service.JapanPostalCodeSearchService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/japan-postal-codes")
public class JapanPostalCodeController {
    private final JapanPostalCodeSearchService japanPostalCodeSearchService;

    public JapanPostalCodeController(JapanPostalCodeSearchService japanPostalCodeSearchService) {
        this.japanPostalCodeSearchService = japanPostalCodeSearchService;
    }

    @GetMapping
    public List<JapanPostalCodeResponse> findByPostalCode(@RequestParam String postalCode) {
        return japanPostalCodeSearchService.findByPostalCode(postalCode);
    }
}
