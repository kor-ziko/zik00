package com.zik00.shop.controller.mypage;

import com.zik00.shop.dto.mypage.AddressCreateRequest;
import com.zik00.shop.dto.mypage.InquiryCommentCreateRequest;
import com.zik00.shop.dto.mypage.InquiryCreateRequest;
import com.zik00.shop.dto.mypage.MypageSection;
import com.zik00.shop.dto.mypage.ProfileUpdateRequest;
import com.zik00.shop.service.mypage.MypageService;
import jakarta.validation.Valid;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class MypageController {
    private static final String VIEW = "mypage/index";
    private static final String REDIRECT_PROFILE = "redirect:/mypage/profile";
    private static final String REDIRECT_PROFILE_ADDRESSES_EDIT = "redirect:/mypage/profile/addresses/edit";
    private static final String REDIRECT_INQUIRIES = "redirect:/mypage/inquiries";
    private static final String REDIRECT_INQUIRIES_NEW = "redirect:/mypage/inquiries/new";
    private static final String SUCCESS_MESSAGE = "successMessage";
    private static final String ERROR_MESSAGE = "errorMessage";

    private static final String PROFILE_UPDATED = "\uD68C\uC6D0\uC815\uBCF4\uAC00 \uC218\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String ADDRESS_CREATED = "\uBC30\uC1A1\uC9C0\uAC00 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String ADDRESS_UPDATED = "\uBC30\uC1A1\uC9C0\uAC00 \uC218\uC815\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String ADDRESS_DELETED = "\uBC30\uC1A1\uC9C0\uAC00 \uC0AD\uC81C\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String INQUIRY_CREATED = "\uBB38\uC758\uAC00 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";
    private static final String COMMENT_CREATED = "\uB313\uAE00\uC774 \uB4F1\uB85D\uB418\uC5C8\uC2B5\uB2C8\uB2E4.";

    private final MypageService mypageService;

    public MypageController(MypageService mypageService) {
        this.mypageService = mypageService;
    }

    @GetMapping("/mypage")
    public String home(Model model) {
        addMypageModel(model, MypageSection.HOME);
        return VIEW;
    }

    @GetMapping("/mypage/orders")
    public String orders(Model model) {
        addMypageModel(model, MypageSection.ORDERS);
        model.addAttribute("purchases", mypageService.getPurchases());
        return VIEW;
    }

    @GetMapping("/mypage/deliveries")
    public String deliveries(Model model) {
        addMypageModel(model, MypageSection.DELIVERIES);
        return VIEW;
    }

    @GetMapping("/mypage/inquiries")
    public String inquiries(Model model) {
        addInquiriesModel(model, false);
        return VIEW;
    }

    @GetMapping("/mypage/inquiries/new")
    public String newInquiry(Model model) {
        addInquiriesModel(model, true);
        return VIEW;
    }

    @GetMapping("/mypage/inquiries/images/{imageUuid}")
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

    private void addInquiriesModel(Model model, boolean inquiryCreateMode) {
        addMypageModel(model, MypageSection.INQUIRIES);
        model.addAttribute("inquiryCreateMode", inquiryCreateMode);
        model.addAttribute("inquiryThreads", mypageService.getInquiryThreads());
        if (!model.containsAttribute("inquiryCreateRequest")) {
            model.addAttribute("inquiryCreateRequest", new InquiryCreateRequest());
        }
    }

    @GetMapping("/mypage/coupons")
    public String coupons(Model model) {
        addMypageModel(model, MypageSection.COUPONS);
        model.addAttribute("coupons", mypageService.getCoupons());
        return VIEW;
    }

    @GetMapping("/mypage/deposits")
    public String deposits(Model model) {
        addMypageModel(model, MypageSection.DEPOSITS);
        return VIEW;
    }

    @GetMapping("/mypage/profile")
    public String profile(Model model) {
        addProfileModel(model, false, false);
        return VIEW;
    }

    @GetMapping("/mypage/profile/edit")
    public String editProfile(Model model) {
        addProfileModel(model, true, false);
        return VIEW;
    }

    @GetMapping("/mypage/profile/addresses/edit")
    public String editDeliveryAddresses(Model model) {
        addProfileModel(model, false, true);
        return VIEW;
    }

    @PostMapping("/mypage/profile")
    public String updateProfile(
            @Valid @ModelAttribute ProfileUpdateRequest profileUpdateRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("profileUpdateRequest", profileUpdateRequest);
            return redirectWithError(redirectAttributes, firstErrorMessage(bindingResult), "redirect:/mypage/profile/edit");
        }

        mypageService.updateProfile(profileUpdateRequest);
        return redirectWithMessage(redirectAttributes, PROFILE_UPDATED, REDIRECT_PROFILE);
    }

    @PostMapping("/mypage/profile/addresses")
    public String addDeliveryAddress(
            @Valid @ModelAttribute AddressCreateRequest addressCreateRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("addressCreateRequest", addressCreateRequest);
            return redirectWithError(redirectAttributes, firstErrorMessage(bindingResult), REDIRECT_PROFILE_ADDRESSES_EDIT);
        }

        try {
            mypageService.addDeliveryAddress(addressCreateRequest);
            return redirectWithMessage(redirectAttributes, ADDRESS_CREATED, REDIRECT_PROFILE);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("addressCreateRequest", addressCreateRequest);
            return redirectWithError(redirectAttributes, exception.getMessage(), REDIRECT_PROFILE_ADDRESSES_EDIT);
        }
    }

    @PostMapping("/mypage/profile/addresses/{addressId}/update")
    public String updateDeliveryAddress(
            @PathVariable long addressId,
            @Valid @ModelAttribute AddressCreateRequest addressCreateRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return redirectWithError(redirectAttributes, firstErrorMessage(bindingResult), REDIRECT_PROFILE_ADDRESSES_EDIT);
        }

        try {
            mypageService.updateDeliveryAddress(addressId, addressCreateRequest);
            return redirectWithMessage(redirectAttributes, ADDRESS_UPDATED, REDIRECT_PROFILE);
        } catch (IllegalArgumentException exception) {
            return redirectWithError(redirectAttributes, exception.getMessage(), REDIRECT_PROFILE_ADDRESSES_EDIT);
        }
    }

    @PostMapping("/mypage/profile/addresses/{addressId}/delete")
    public String deleteDeliveryAddress(
            @PathVariable long addressId,
            RedirectAttributes redirectAttributes
    ) {
        mypageService.deleteDeliveryAddress(addressId);
        return redirectWithMessage(redirectAttributes, ADDRESS_DELETED, REDIRECT_PROFILE);
    }

    @PostMapping("/mypage/inquiries")
    public String addInquiry(
            @Valid @ModelAttribute InquiryCreateRequest inquiryCreateRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("inquiryCreateRequest", copyInquiryText(inquiryCreateRequest));
            return redirectWithError(redirectAttributes, firstErrorMessage(bindingResult), REDIRECT_INQUIRIES_NEW);
        }

        try {
            mypageService.addInquiry(inquiryCreateRequest);
            return redirectWithMessage(redirectAttributes, INQUIRY_CREATED, REDIRECT_INQUIRIES);
        } catch (IllegalArgumentException | IllegalStateException exception) {
            redirectAttributes.addFlashAttribute("inquiryCreateRequest", copyInquiryText(inquiryCreateRequest));
            return redirectWithError(redirectAttributes, exception.getMessage(), REDIRECT_INQUIRIES_NEW);
        }
    }

    @PostMapping("/mypage/inquiries/{inquiryId}/comments")
    public String addInquiryComment(
            @PathVariable long inquiryId,
            @Valid @ModelAttribute InquiryCommentCreateRequest inquiryCommentCreateRequest,
            BindingResult bindingResult,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return redirectWithError(redirectAttributes, firstErrorMessage(bindingResult), REDIRECT_INQUIRIES);
        }

        mypageService.addInquiryComment(inquiryId, inquiryCommentCreateRequest);
        return redirectWithMessage(redirectAttributes, COMMENT_CREATED, REDIRECT_INQUIRIES);
    }

    private void addProfileModel(Model model, boolean profileEditMode, boolean addressEditMode) {
        addMypageModel(model, MypageSection.PROFILE);
        model.addAttribute("profileEditMode", profileEditMode);
        model.addAttribute("addressEditMode", addressEditMode);
        model.addAttribute("deliveryAddresses", mypageService.getDeliveryAddresses());
        if (!model.containsAttribute("profileUpdateRequest")) {
            model.addAttribute("profileUpdateRequest", mypageService.getProfileUpdateRequest());
        }
        if (!model.containsAttribute("addressCreateRequest")) {
            model.addAttribute("addressCreateRequest", mypageService.getAddressCreateRequest());
        }
    }

    private void addMypageModel(Model model, MypageSection activeSection) {
        model.addAttribute("activeSection", activeSection);
        model.addAttribute("activeSectionName", activeSection.name());
        model.addAttribute("menuItems", mypageService.getMenuItems(activeSection));
        model.addAttribute("summary", mypageService.getSummary());
        model.addAttribute("user", mypageService.getCurrentUser());
    }

    private String redirectWithMessage(RedirectAttributes redirectAttributes, String message, String redirectUrl) {
        redirectAttributes.addFlashAttribute(SUCCESS_MESSAGE, message);
        return redirectUrl;
    }

    private String redirectWithError(RedirectAttributes redirectAttributes, String message, String redirectUrl) {
        redirectAttributes.addFlashAttribute(ERROR_MESSAGE, message);
        return redirectUrl;
    }

    private String firstErrorMessage(BindingResult bindingResult) {
        if (!bindingResult.hasErrors()) {
            return "\uC785\uB825\uAC12\uC744 \uD655\uC778\uD574\uC8FC\uC138\uC694.";
        }
        return bindingResult.getAllErrors().get(0).getDefaultMessage();
    }

    private InquiryCreateRequest copyInquiryText(InquiryCreateRequest source) {
        InquiryCreateRequest copy = new InquiryCreateRequest();
        copy.setTitle(source.getTitle());
        copy.setContent(source.getContent());
        return copy;
    }
}
