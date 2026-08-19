package com.zik00.admin.controller.member_management.withdrawn_member;

import com.zik00.admin.dto.member_management.withdrawn_member.WithdrawnMemberResponse;
import com.zik00.admin.service.member_management.withdrawn_member.WithdrawnMemberService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-management/withdrawn-members")
public class WithdrawnMemberController {
    private final WithdrawnMemberService service;
    public WithdrawnMemberController(WithdrawnMemberService service) { this.service = service; }
    @GetMapping public List<WithdrawnMemberResponse> findAll() { return service.findAll(); }
}
