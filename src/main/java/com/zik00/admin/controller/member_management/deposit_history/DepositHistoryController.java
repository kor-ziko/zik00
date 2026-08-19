package com.zik00.admin.controller.member_management.deposit_history;

import com.zik00.admin.dto.member_management.deposit_history.DepositHistoryResponse;
import com.zik00.admin.service.member_management.deposit_history.DepositHistoryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-management/deposit-histories")
public class DepositHistoryController {
    private final DepositHistoryService service;
    public DepositHistoryController(DepositHistoryService service) { this.service = service; }
    @GetMapping public List<DepositHistoryResponse> findAll() { return service.findAll(); }
}
