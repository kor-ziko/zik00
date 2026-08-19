package com.zik00.shop.controller.service_intro;

import com.zik00.shop.dto.service_intro.ServiceIntroResponse;
import com.zik00.shop.service.service_intro.ServiceIntroService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/service-intro")
public class ServiceIntroController {
    private final ServiceIntroService serviceIntroService;

    public ServiceIntroController(ServiceIntroService serviceIntroService) {
        this.serviceIntroService = serviceIntroService;
    }

    @GetMapping
    public ServiceIntroResponse findPage() {
        return serviceIntroService.findPage();
    }
}
