package com.zik00.admin.controller.member_management.deposit_request;

import com.zik00.admin.dto.member_management.deposit_request.DepositRequestProcessRequest;
import com.zik00.admin.dto.member_management.deposit_request.DepositRequestResponse;
import com.zik00.admin.service.member_management.deposit_request.DepositRequestService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-management/deposit-requests")
public class DepositRequestController {
    private final DepositRequestService service;
    public DepositRequestController(DepositRequestService service) { this.service = service; }
    @GetMapping public List<DepositRequestResponse> findAll() { return service.findAll(); }
    @PostMapping("/{id}/approve") public DepositRequestResponse approve(@PathVariable Long id, @RequestBody DepositRequestProcessRequest request) { return service.approve(id, request); }
    @PostMapping("/{id}/reject") public DepositRequestResponse reject(@PathVariable Long id, @RequestBody DepositRequestProcessRequest request) { return service.reject(id, request); }
}
