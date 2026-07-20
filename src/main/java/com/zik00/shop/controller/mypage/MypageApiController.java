package com.zik00.shop.controller.mypage;

import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import com.zik00.shop.dto.mypage.CouponResponse;
import com.zik00.shop.dto.mypage.AddressCreateRequest;
import com.zik00.shop.dto.mypage.DeliveryAddressResponse;
import com.zik00.shop.dto.mypage.InquiryThread;
import com.zik00.shop.dto.mypage.InquiryCreateRequest;
import com.zik00.shop.dto.mypage.MypageProfileResponse;
import com.zik00.shop.dto.mypage.MypageSummary;
import com.zik00.shop.dto.mypage.PurchaseResponse;
import com.zik00.shop.dto.mypage.ProfileUpdateRequest;
import com.zik00.shop.service.mypage.MypageService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/mypage")
public class MypageApiController {
    private final MypageService mypageService;

    public MypageApiController(MypageService mypageService) {
        this.mypageService = mypageService;
    }

    @GetMapping
    public DashboardResponse dashboard() {
        MypageService.DashboardData dashboard = mypageService.getDashboard();
        return new DashboardResponse(
                dashboard.summary(),
                dashboard.profile(),
                dashboard.recentOrders(),
                dashboard.coupons()
        );
    }

    @GetMapping("/orders")
    public List<PurchaseResponse> orders() {
        return mypageService.getPurchases();
    }

    @GetMapping("/deliveries")
    public List<PurchaseResponse> deliveries() {
        return mypageService.getPurchases();
    }

    @GetMapping("/inquiries")
    public List<InquiryThread> inquiries() {
        return mypageService.getInquiryThreads();
    }

    @GetMapping("/inquiries/images/{imageUuid}")
    public ResponseEntity<Resource> inquiryImage(@PathVariable String imageUuid) {
        return mypageService.getInquiryImageDownload(imageUuid)
                .map(image -> ResponseEntity.ok()
                        .contentType(MediaType.parseMediaType(image.contentType()))
                        .contentLength(image.contentLength())
                        .cacheControl(CacheControl.noStore())
                        .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                                .filename(image.fileName())
                                .build()
                                .toString())
                        .header("X-Content-Type-Options", "nosniff")
                        .body((Resource) new FileSystemResource(image.imagePath())))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping(value = "/inquiries", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> addInquiry(
            @Valid @ModelAttribute InquiryCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            List<String> messages = bindingResult.getAllErrors().stream()
                    .map(error -> error.getDefaultMessage())
                    .distinct()
                    .toList();
            return ResponseEntity.badRequest().body(new ErrorResponse(messages));
        }

        try {
            mypageService.addInquiry(request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException | IllegalStateException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(List.of(exception.getMessage())));
        }
    }

    @GetMapping("/coupons")
    public List<CouponResponse> coupons() {
        return mypageService.getCoupons();
    }

    @GetMapping("/deposits")
    public MypageSummary deposits() {
        return mypageService.getSummary();
    }

    @GetMapping("/profile")
    public ProfileResponse profile() {
        return new ProfileResponse(
                mypageService.getCurrentUser(),
                mypageService.getDeliveryAddresses()
        );
    }

    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(
            @Valid @RequestBody ProfileUpdateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return validationError(bindingResult);
        }
        mypageService.updateProfile(request);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/profile/addresses")
    public ResponseEntity<?> addAddress(
            @Valid @RequestBody AddressCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return validationError(bindingResult);
        }
        try {
            mypageService.addDeliveryAddress(request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(List.of(exception.getMessage())));
        }
    }

    @PutMapping("/profile/addresses/{addressId}")
    public ResponseEntity<?> updateAddress(
            @PathVariable long addressId,
            @Valid @RequestBody AddressCreateRequest request,
            BindingResult bindingResult
    ) {
        if (bindingResult.hasErrors()) {
            return validationError(bindingResult);
        }
        try {
            mypageService.updateDeliveryAddress(addressId, request);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(List.of(exception.getMessage())));
        }
    }

    @DeleteMapping("/profile/addresses/{addressId}")
    public ResponseEntity<?> deleteAddress(@PathVariable long addressId) {
        try {
            mypageService.deleteDeliveryAddress(addressId);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest().body(new ErrorResponse(List.of(exception.getMessage())));
        }
    }

    private ResponseEntity<ErrorResponse> validationError(BindingResult bindingResult) {
        List<String> messages = bindingResult.getAllErrors().stream()
                .map(error -> error.getDefaultMessage())
                .distinct()
                .toList();
        return ResponseEntity.badRequest().body(new ErrorResponse(messages));
    }

    public record DashboardResponse(
            MypageSummary summary,
            MypageProfileResponse profile,
            List<PurchaseResponse> recentOrders,
            List<CouponResponse> coupons
    ) {
    }

    public record ProfileResponse(
            MypageProfileResponse profile,
            List<DeliveryAddressResponse> addresses
    ) {
    }

    public record ErrorResponse(List<String> messages) {
    }
}
