package com.zik00.shop.controller.auth.registration_terms;

import com.zik00.shop.dto.auth.registration_terms.RegistrationTermResponse;
import com.zik00.shop.service.auth.registration_terms.RegistrationTermService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/registration-terms")
public class RegistrationTermController {
    private final RegistrationTermService service;

    public RegistrationTermController(RegistrationTermService service) {
        this.service = service;
    }

    @GetMapping
    public List<RegistrationTermResponse> findActiveTerms() {
        return service.findActiveTerms();
    }
}
