package com.zik00.shop.controller;

import java.util.List;

import com.zik00.shop.dto.JapanPostalCodeResponse;
import com.zik00.shop.repository.JapanPostalCodeRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/japan-postal-codes")
public class JapanPostalCodeController {
    private final JapanPostalCodeRepository japanPostalCodeRepository;

    public JapanPostalCodeController(JapanPostalCodeRepository japanPostalCodeRepository) {
        this.japanPostalCodeRepository = japanPostalCodeRepository;
    }

    @GetMapping
    public List<JapanPostalCodeResponse> findByPostalCode(@RequestParam String postalCode) {
        String normalizedPostalCode = postalCode.replaceAll("\\D", "");
        if (normalizedPostalCode.length() != 7) {
            return List.of();
        }

        return japanPostalCodeRepository.findByCode(normalizedPostalCode).stream()
                .map(JapanPostalCodeResponse::new)
                .toList();
    }
}
