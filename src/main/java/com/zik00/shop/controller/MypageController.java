package com.zik00.shop.controller;

import com.zik00.shop.dto.AddressCreateRequest;
import com.zik00.shop.dto.InquiryCommentCreateRequest;
import com.zik00.shop.dto.InquiryCreateRequest;
import com.zik00.shop.dto.MypageSection;
import com.zik00.shop.dto.ProfileUpdateRequest;
import com.zik00.shop.service.MypageService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/mypage")
public class MypageController {
    private static final String VIEW = "mypage/index";
    private static final String REDIRECT_PROFILE = "redirect:/mypage/profile";
    private static final String REDIRECT_INQUIRIES = "redirect:/mypage/inquiries";
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

    @GetMapping({"", "/"})
    public String home(Model model) {
        addMypageModel(model, MypageSection.HOME);
        return VIEW;
    }

    @GetMapping("/orders")
    public String orders(Model model) {
        addMypageModel(model, MypageSection.ORDERS);
        model.addAttribute("purchases", mypageService.getPurchases());
        return VIEW;
    }

    @GetMapping("/deliveries")
    public String deliveries(Model model) {
        addMypageModel(model, MypageSection.DELIVERIES);
        return VIEW;
    }

    @GetMapping("/inquiries")
    public String inquiries(Model model) {
        addMypageModel(model, MypageSection.INQUIRIES);
        model.addAttribute("inquiryThreads", mypageService.getInquiryThreads());
        if (!model.containsAttribute("inquiryCreateRequest")) {
            model.addAttribute("inquiryCreateRequest", new InquiryCreateRequest());
        }
        return VIEW;
    }

    @GetMapping("/coupons")
    public String coupons(Model model) {
        addMypageModel(model, MypageSection.COUPONS);
        model.addAttribute("coupons", mypageService.getCoupons());
        return VIEW;
    }

    @GetMapping("/deposits")
    public String deposits(Model model) {
        addMypageModel(model, MypageSection.DEPOSITS);
        return VIEW;
    }

    @GetMapping("/profile")
    public String profile(Model model) {
        addProfileModel(model, false);
        return VIEW;
    }

    @GetMapping("/profile/edit")
    public String editProfile(Model model) {
        addProfileModel(model, true);
        return VIEW;
    }

    @PostMapping("/profile")
    public String updateProfile(
            @ModelAttribute ProfileUpdateRequest profileUpdateRequest,
            RedirectAttributes redirectAttributes
    ) {
        mypageService.updateProfile(profileUpdateRequest);
        return redirectWithMessage(redirectAttributes, PROFILE_UPDATED, REDIRECT_PROFILE);
    }

    @PostMapping("/profile/addresses")
    public String addDeliveryAddress(
            @ModelAttribute AddressCreateRequest addressCreateRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            mypageService.addDeliveryAddress(addressCreateRequest);
            return redirectWithMessage(redirectAttributes, ADDRESS_CREATED, REDIRECT_PROFILE);
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("addressCreateRequest", addressCreateRequest);
            return redirectWithError(redirectAttributes, exception.getMessage(), REDIRECT_PROFILE);
        }
    }

    @PostMapping("/profile/addresses/{addressId}/update")
    public String updateDeliveryAddress(
            @PathVariable long addressId,
            @ModelAttribute AddressCreateRequest addressCreateRequest,
            RedirectAttributes redirectAttributes
    ) {
        try {
            mypageService.updateDeliveryAddress(addressId, addressCreateRequest);
            return redirectWithMessage(redirectAttributes, ADDRESS_UPDATED, REDIRECT_PROFILE);
        } catch (IllegalArgumentException exception) {
            return redirectWithError(redirectAttributes, exception.getMessage(), REDIRECT_PROFILE);
        }
    }

    @PostMapping("/profile/addresses/{addressId}/delete")
    public String deleteDeliveryAddress(
            @PathVariable long addressId,
            RedirectAttributes redirectAttributes
    ) {
        mypageService.deleteDeliveryAddress(addressId);
        return redirectWithMessage(redirectAttributes, ADDRESS_DELETED, REDIRECT_PROFILE);
    }

    @PostMapping("/inquiries")
    public String addInquiry(
            @ModelAttribute InquiryCreateRequest inquiryCreateRequest,
            RedirectAttributes redirectAttributes
    ) {
        mypageService.addInquiry(inquiryCreateRequest);
        return redirectWithMessage(redirectAttributes, INQUIRY_CREATED, REDIRECT_INQUIRIES);
    }

    @PostMapping("/inquiries/{inquiryId}/comments")
    public String addInquiryComment(
            @PathVariable long inquiryId,
            @ModelAttribute InquiryCommentCreateRequest inquiryCommentCreateRequest,
            RedirectAttributes redirectAttributes
    ) {
        mypageService.addInquiryComment(inquiryId, inquiryCommentCreateRequest);
        return redirectWithMessage(redirectAttributes, COMMENT_CREATED, REDIRECT_INQUIRIES);
    }

    private void addProfileModel(Model model, boolean profileEditMode) {
        addMypageModel(model, MypageSection.PROFILE);
        model.addAttribute("profileEditMode", profileEditMode);
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
}
