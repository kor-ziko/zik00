package com.zik00.admin.service;

import com.zik00.admin.domain.CouponTemplate;
import com.zik00.admin.dto.AdminCouponTemplateCreateRequest;
import com.zik00.admin.dto.AdminCouponTemplateResponse;
import com.zik00.admin.dto.AdminGuestCouponIssueRequest;
import com.zik00.admin.dto.AdminIssuedCouponResponse;
import com.zik00.admin.dto.AdminMemberCouponIssueRequest;
import com.zik00.admin.repository.AdminCouponRepository;
import com.zik00.admin.repository.CouponTemplateRepository;
import com.zik00.shop.domain.Coupon;
import com.zik00.shop.domain.User;
import com.zik00.shop.repository.UserRepository;
import java.security.SecureRandom;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminCouponService {
    private static final char[] COUPON_CODE_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789".toCharArray();
    private static final int COUPON_CODE_LENGTH = 12;

    private final CouponTemplateRepository couponTemplateRepository;
    private final AdminCouponRepository adminCouponRepository;
    private final UserRepository userRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public AdminCouponService(
            CouponTemplateRepository couponTemplateRepository,
            AdminCouponRepository adminCouponRepository,
            UserRepository userRepository
    ) {
        this.couponTemplateRepository = couponTemplateRepository;
        this.adminCouponRepository = adminCouponRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<AdminCouponTemplateResponse> findTemplates() {
        return couponTemplateRepository.findAll(Sort.by(Sort.Direction.DESC, "id"))
                .stream()
                .map(AdminCouponTemplateResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminIssuedCouponResponse> findIssuedCoupons() {
        return adminCouponRepository.findIssuedCoupons()
                .stream()
                .map(AdminIssuedCouponResponse::from)
                .toList();
    }

    @Transactional
    public AdminCouponTemplateResponse createTemplate(AdminCouponTemplateCreateRequest request) {
        if (request.startedDate() != null
                && request.expiredDate() != null
                && request.startedDate().isAfter(request.expiredDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 만료일보다 늦을 수 없습니다.");
        }

        CouponTemplate template = new CouponTemplate(
                request.couponName(),
                request.discountType(),
                request.discountValue(),
                request.minimumOrderAmount(),
                request.startedDate(),
                request.expiredDate(),
                request.targetType(),
                request.active()
        );
        return AdminCouponTemplateResponse.from(couponTemplateRepository.save(template));
    }

    @Transactional
    public List<AdminIssuedCouponResponse> issueMemberCoupon(AdminMemberCouponIssueRequest request) {
        if (request.memberIds() == null || request.memberIds().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "회원을 선택해주세요.");
        }

        CouponIssueSpec issueSpec = resolveIssueSpec(request);
        Map<Long, User> usersById = userRepository.findAllById(request.memberIds()).stream()
                .collect(Collectors.toMap(User::getMemberId, Function.identity()));
        List<Coupon> coupons = request.memberIds()
                .stream()
                .map(memberId -> requireUser(usersById, memberId))
                .map(user -> Coupon.issueToMember(
                        issueSpec.couponTemplateId(),
                        user.getMemberId(),
                        issueSpec.couponName(),
                        issueSpec.discountType(),
                        issueSpec.discountValue(),
                        issueSpec.minimumOrderAmount(),
                        issueSpec.startedDate(),
                        issueSpec.expiredDate()
                ))
                .toList();
        return adminCouponRepository.saveAll(coupons).stream()
                .map(AdminIssuedCouponResponse::from)
                .toList();
    }

    @Transactional
    public AdminIssuedCouponResponse issueGuestCoupon(AdminGuestCouponIssueRequest request) {
        CouponIssueSpec issueSpec = resolveIssueSpec(request);
        String couponCode = hasText(request.couponCode()) ? request.couponCode().trim() : generateCouponCode();
        Coupon coupon = Coupon.issueToGuest(
                issueSpec.couponTemplateId(),
                request.guestIdentifier(),
                couponCode,
                issueSpec.couponName(),
                issueSpec.discountType(),
                issueSpec.discountValue(),
                issueSpec.minimumOrderAmount(),
                issueSpec.startedDate(),
                issueSpec.expiredDate()
        );
        return AdminIssuedCouponResponse.from(adminCouponRepository.save(coupon));
    }

    @Transactional
    public void deleteIssuedCoupon(Long couponId) {
        if (!adminCouponRepository.existsById(couponId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "발급된 쿠폰을 찾을 수 없습니다.");
        }
        adminCouponRepository.deleteById(couponId);
    }

    private CouponIssueSpec resolveIssueSpec(AdminMemberCouponIssueRequest request) {
        if (request.couponTemplateId() != null) {
            return CouponIssueSpec.from(findTemplate(request.couponTemplateId()));
        }
        return CouponIssueSpec.custom(
                request.couponName(),
                request.discountType(),
                request.discountValue(),
                request.minimumOrderAmount(),
                request.startedDate(),
                request.expiredDate()
        );
    }

    private CouponIssueSpec resolveIssueSpec(AdminGuestCouponIssueRequest request) {
        if (request.couponTemplateId() != null) {
            return CouponIssueSpec.from(findTemplate(request.couponTemplateId()));
        }
        return CouponIssueSpec.custom(
                request.couponName(),
                request.discountType(),
                request.discountValue(),
                request.minimumOrderAmount(),
                request.startedDate(),
                request.expiredDate()
        );
    }

    private CouponTemplate findTemplate(Long couponTemplateId) {
        return couponTemplateRepository.findById(couponTemplateId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "쿠폰 종류를 찾을 수 없습니다."));
    }

    private User findUser(Long memberId) {
        return userRepository.findById(memberId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "회원을 찾을 수 없습니다."));
    }

    private User requireUser(Map<Long, User> usersById, Long memberId) {
        User user = usersById.get(memberId);
        if (user == null) {
            return findUser(memberId);
        }
        return user;
    }

    private String generateCouponCode() {
        StringBuilder builder = new StringBuilder(COUPON_CODE_LENGTH);
        for (int index = 0; index < COUPON_CODE_LENGTH; index++) {
            builder.append(COUPON_CODE_CHARS[secureRandom.nextInt(COUPON_CODE_CHARS.length)]);
        }
        return builder.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    private record CouponIssueSpec(
            Long couponTemplateId,
            String couponName,
            String discountType,
            int discountValue,
            int minimumOrderAmount,
            java.time.LocalDate startedDate,
            java.time.LocalDate expiredDate
    ) {
        static CouponIssueSpec from(CouponTemplate template) {
            return new CouponIssueSpec(
                    template.getId(),
                    template.getCouponName(),
                    template.getDiscountType(),
                    template.getDiscountValue(),
                    template.getMinimumOrderAmount(),
                    template.getStartedDate(),
                    template.getExpiredDate()
            );
        }

        static CouponIssueSpec custom(
                String couponName,
                String discountType,
                int discountValue,
                int minimumOrderAmount,
                java.time.LocalDate startedDate,
                java.time.LocalDate expiredDate
        ) {
            if (couponName == null || couponName.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "쿠폰명을 입력해주세요.");
            }
            if (discountType == null || discountType.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "할인 타입을 선택해주세요.");
            }
            if (startedDate != null && expiredDate != null && startedDate.isAfter(expiredDate)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "시작일은 만료일보다 늦을 수 없습니다.");
            }
            return new CouponIssueSpec(
                    null,
                    couponName.trim(),
                    discountType.trim(),
                    discountValue,
                    minimumOrderAmount,
                    startedDate,
                    expiredDate
            );
        }
    }
}
