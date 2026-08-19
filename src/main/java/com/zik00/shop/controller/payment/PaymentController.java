package com.zik00.shop.controller.payment;

import com.zik00.shop.dto.payment.PaymentPrepareRequest;
import com.zik00.shop.dto.payment.PaymentPrepareResponse;
import com.zik00.shop.dto.payment.PaymentStartRequest;
import com.zik00.shop.dto.payment.PaymentStartResponse;
import com.zik00.shop.service.payment.PaymentService;
import com.zik00.shop.service.payment.SbPaymentGateway;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payment")
public class PaymentController {
    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/prepare")
    public PaymentPrepareResponse prepare(@Valid @RequestBody PaymentPrepareRequest request) {
        return paymentService.prepare(request);
    }

    @PostMapping("/start")
    public PaymentStartResponse start(@Valid @RequestBody PaymentStartRequest request) {
        return paymentService.start(request.paymentId(), request.paymentMethod());
    }

    @PostMapping(value = "/sbps/callback", consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public ResponseEntity<String> callback(@RequestParam MultiValueMap<String, String> parameters) {
        try {
            paymentService.processSbpsResult(firstValues(parameters));
            return ResponseEntity.ok().contentType(MediaType.TEXT_PLAIN).body("OK,");
        } catch (RuntimeException exception) {
            return ResponseEntity.badRequest().contentType(MediaType.TEXT_PLAIN).body("NG,invalid result");
        }
    }

    @RequestMapping(value = "/sbps/return/{outcome}", method = {RequestMethod.GET, RequestMethod.POST})
    public ResponseEntity<Void> result(
            @PathVariable String outcome,
            @RequestParam MultiValueMap<String, String> parameters
    ) {
        Map<String, String> values = firstValues(parameters);
        String paymentId = values.getOrDefault("order_id", "");
        SbPaymentGateway.PaymentState state = "success".equals(outcome)
                ? SbPaymentGateway.PaymentState.PENDING : SbPaymentGateway.PaymentState.FAILED;
        String message = "cancel".equals(outcome) ? "결제가 취소되었습니다."
                : "error".equals(outcome) ? "결제 처리 중 오류가 발생했습니다." : "";

        if (values.containsKey("sps_hashcode")) {
            try {
                PaymentService.SbpsProcessingResult processed = paymentService.processSbpsResult(values);
                paymentId = processed.paymentId();
                state = processed.state();
                if (state == SbPaymentGateway.PaymentState.FAILED) message = "결제가 승인되지 않았습니다.";
            } catch (RuntimeException exception) {
                state = SbPaymentGateway.PaymentState.FAILED;
                message = "결제 결과를 확인하지 못했습니다.";
            }
        }
        return ResponseEntity.status(303)
                .location(URI.create(paymentService.resultUrl(paymentId, state, message)))
                .build();
    }

    private Map<String, String> firstValues(MultiValueMap<String, String> parameters) {
        Map<String, String> values = new LinkedHashMap<>();
        parameters.forEach((name, entries) -> values.put(name, entries.isEmpty() ? "" : entries.getFirst()));
        return values;
    }
}
