package com.zik00.admin.controller.member_management.member_list;

import com.zik00.admin.dto.member_management.member_list.MemberListResponse;
import com.zik00.admin.service.member_management.member_list.MemberListService;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/member-management/members")
public class MemberListController {
    private final MemberListService service;
    public MemberListController(MemberListService service) { this.service = service; }
    @GetMapping public List<MemberListResponse> findAll() { return service.findAll(); }

    @DeleteMapping("/{memberId}")
    public ResponseEntity<?> withdraw(@PathVariable long memberId) {
        try {
            service.withdraw(memberId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
        }
    }

    private record ErrorResponse(String message) {
    }
}
