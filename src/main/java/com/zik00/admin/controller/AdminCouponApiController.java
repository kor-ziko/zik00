package com.zik00.admin.controller;

import com.zik00.admin.dto.AdminCouponTemplateCreateRequest;
import com.zik00.admin.dto.AdminCouponTemplateResponse;
import com.zik00.admin.dto.AdminGuestCouponIssueRequest;
import com.zik00.admin.dto.AdminIssuedCouponResponse;
import com.zik00.admin.dto.AdminMemberCouponIssueRequest;
import com.zik00.admin.service.AdminCouponService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/coupons")
public class AdminCouponApiController {
    private final AdminCouponService adminCouponService;

    public AdminCouponApiController(AdminCouponService adminCouponService) {
        this.adminCouponService = adminCouponService;
    }

    @GetMapping("/templates")
    public List<AdminCouponTemplateResponse> findTemplates() {
        return adminCouponService.findTemplates();
    }

    @PostMapping("/templates")
    public AdminCouponTemplateResponse createTemplate(
            @Valid @RequestBody AdminCouponTemplateCreateRequest request
    ) {
        return adminCouponService.createTemplate(request);
    }

    @GetMapping("/issued")
    public List<AdminIssuedCouponResponse> findIssuedCoupons() {
        return adminCouponService.findIssuedCoupons();
    }

    @PostMapping("/issued/members")
    public List<AdminIssuedCouponResponse> issueMemberCoupon(
            @Valid @RequestBody AdminMemberCouponIssueRequest request
    ) {
        return adminCouponService.issueMemberCoupon(request);
    }

    @PostMapping("/issued/guests")
    public AdminIssuedCouponResponse issueGuestCoupon(
            @Valid @RequestBody AdminGuestCouponIssueRequest request
    ) {
        return adminCouponService.issueGuestCoupon(request);
    }

    @DeleteMapping("/issued/{couponId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIssuedCoupon(@PathVariable Long couponId) {
        adminCouponService.deleteIssuedCoupon(couponId);
    }
}
