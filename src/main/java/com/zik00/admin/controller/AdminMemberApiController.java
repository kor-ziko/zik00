package com.zik00.admin.controller;

import com.zik00.admin.dto.AdminMemberDetailResponse;
import com.zik00.admin.dto.AdminMemberSummaryResponse;
import com.zik00.admin.service.AdminMemberService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/members")
public class AdminMemberApiController {
    private final AdminMemberService adminMemberService;

    public AdminMemberApiController(AdminMemberService adminMemberService) {
        this.adminMemberService = adminMemberService;
    }

    @GetMapping
    public List<AdminMemberSummaryResponse> findMembers() {
        return adminMemberService.findMembers();
    }

    @GetMapping("/{memberId}")
    public AdminMemberDetailResponse findMember(@PathVariable Long memberId) {
        return adminMemberService.findMember(memberId);
    }
}
