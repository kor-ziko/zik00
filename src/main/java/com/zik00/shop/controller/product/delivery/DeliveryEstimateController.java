package com.zik00.shop.controller.product.delivery;

import com.zik00.shop.dto.product.delivery.DeliveryEstimateRequest;
import com.zik00.shop.dto.product.delivery.DeliveryEstimateResponse;
import com.zik00.shop.service.product.delivery.DeliveryEstimateService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/delivery")
public class DeliveryEstimateController {
    private final DeliveryEstimateService service;

    public DeliveryEstimateController(DeliveryEstimateService service) {
        this.service = service;
    }

    @GetMapping("/estimate")
    public DeliveryEstimateResponse estimate(@Valid @ModelAttribute DeliveryEstimateRequest request) {
        return service.estimate(request);
    }
}
