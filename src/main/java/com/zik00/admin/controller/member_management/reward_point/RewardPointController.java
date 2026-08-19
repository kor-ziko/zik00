package com.zik00.admin.controller.member_management.reward_point;

import com.zik00.admin.dto.member_management.reward_point.RewardPointAdjustmentRequest;
import com.zik00.admin.dto.member_management.reward_point.RewardPointHistoryResponse;
import com.zik00.admin.dto.member_management.reward_point.RewardPointMemberResponse;
import com.zik00.admin.service.member_management.reward_point.RewardPointService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-management/reward-points")
public class RewardPointController {
    private final RewardPointService service;
    public RewardPointController(RewardPointService service) { this.service = service; }
    @GetMapping("/members") public List<RewardPointMemberResponse> findMembers() { return service.findMembers(); }
    @GetMapping("/histories") public List<RewardPointHistoryResponse> findHistories() { return service.findHistories(); }
    @PostMapping("/adjustments") public RewardPointHistoryResponse adjust(@Valid @RequestBody RewardPointAdjustmentRequest request) { return service.adjust(request); }
}
