package com.zik00.shop.controller.product.pricing;

import com.zik00.shop.dto.product.pricing.LandedPriceEstimateRequest;
import com.zik00.shop.dto.product.pricing.LandedPriceEstimateResponse;
import com.zik00.shop.dto.product.pricing.OperatingExchangeRateResponse;
import com.zik00.shop.service.payment.OperatingExchangeRateService;
import com.zik00.shop.service.product.pricing.JapanCustomsDataService;
import com.zik00.shop.service.product.pricing.LandedPriceService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products/pricing")
public class LandedPriceController {
    private final LandedPriceService landedPriceService;
    private final OperatingExchangeRateService exchangeRateService;
    private final JapanCustomsDataService customsDataService;

    public LandedPriceController(
            LandedPriceService landedPriceService,
            OperatingExchangeRateService exchangeRateService,
            JapanCustomsDataService customsDataService
    ) {
        this.landedPriceService = landedPriceService;
        this.exchangeRateService = exchangeRateService;
        this.customsDataService = customsDataService;
    }

    @GetMapping("/rate")
    public OperatingExchangeRateResponse rate() {
        JapanCustomsDataService.SnapshotView view = customsDataService.current();
        return new OperatingExchangeRateResponse(
                "KRW", "JPY", exchangeRateService.currentRate(), view.snapshot().rateFrom(),
                view.snapshot().rateTo(), view.stale()
        );
    }

    @PostMapping("/estimate")
    public LandedPriceEstimateResponse estimate(@Valid @RequestBody LandedPriceEstimateRequest request) {
        return landedPriceService.estimate(request);
    }
}
