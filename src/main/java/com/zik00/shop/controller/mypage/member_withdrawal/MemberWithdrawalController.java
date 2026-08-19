package com.zik00.shop.controller.mypage.member_withdrawal;

import com.zik00.shop.dto.mypage.member_withdrawal.MemberWithdrawalRequest;
import com.zik00.shop.service.auth.JwtSessionService;
import com.zik00.shop.service.mypage.member_withdrawal.MemberWithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/mypage/profile/withdrawal")
public class MemberWithdrawalController {
    private final MemberWithdrawalService memberWithdrawalService;
    private final JwtSessionService jwtSessionService;

    public MemberWithdrawalController(
            MemberWithdrawalService memberWithdrawalService,
            JwtSessionService jwtSessionService
    ) {
        this.memberWithdrawalService = memberWithdrawalService;
        this.jwtSessionService = jwtSessionService;
    }

    @DeleteMapping
    public ResponseEntity<?> withdraw(
            @Valid @RequestBody MemberWithdrawalRequest request,
            BindingResult bindingResult,
            HttpServletRequest servletRequest,
            HttpServletResponse servletResponse
    ) {
        if (bindingResult.hasErrors()) {
            List<String> messages = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .distinct()
                    .toList();
            return ResponseEntity.badRequest().body(new ErrorResponse(messages));
        }

        try {
            memberWithdrawalService.withdraw(request);
            jwtSessionService.logout(servletRequest, servletResponse);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(List.of(exception.getMessage())));
        }
    }

    private record ErrorResponse(List<String> messages) {
    }
}
