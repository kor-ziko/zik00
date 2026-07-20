package com.zik00.shop.service.auth;

import java.util.List;

import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.User;
import com.zik00.shop.dto.auth.AdditionalInfoRequest;
import com.zik00.shop.dto.mypage.JapanPostalCodeResponse;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.service.JapanPostalCodeSearchService;
import com.zik00.shop.util.PhoneNumberFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegistrationService {
    private final AuthenticatedUserService authenticatedUserService;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final JapanPostalCodeSearchService postalCodeSearchService;

    public RegistrationService(
            AuthenticatedUserService authenticatedUserService,
            DeliveryAddressRepository deliveryAddressRepository,
            JapanPostalCodeSearchService postalCodeSearchService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.deliveryAddressRepository = deliveryAddressRepository;
        this.postalCodeSearchService = postalCodeSearchService;
    }

    public boolean isCurrentUserRegistrationComplete() {
        return isRegistrationComplete(authenticatedUserService.getCurrentUser());
    }

    public boolean isRegistrationComplete(User user) {
        return user.hasCompletedRegistration()
                && deliveryAddressRepository.existsByMemberId(user.getMemberId());
    }

    @Transactional
    public void completeRegistration(AdditionalInfoRequest request) {
        User user = authenticatedUserService.getCurrentUser();
        if (isRegistrationComplete(user)) {
            return;
        }

        JapanPostalCodeResponse selectedAddress = verifySelectedAddress(request);
        String telephone = PhoneNumberFormatter.formatTelephone(request.getTelephone());
        String mobilePhone = PhoneNumberFormatter.formatMobilePhone(request.getMobilePhone());
        user.completeRegistration(
                request.getNameKanji(),
                request.getNameKatakana(),
                request.getBirthDate(),
                request.getGender(),
                request.getNickname(),
                telephone,
                mobilePhone,
                request.isAlarmConsent()
        );

        if (!deliveryAddressRepository.existsByMemberId(user.getMemberId())) {
            deliveryAddressRepository.save(new DeliveryAddress(
                    0,
                    user.getMemberId(),
                    "기본 배송지",
                    request.getNameKanji(),
                    mobilePhone,
                    selectedAddress.getZipCode(),
                    selectedAddress.getProvince(),
                    joinAddress(selectedAddress.getDetailAddress(), request.getDetailAddress()),
                    true
            ));
        }
    }

    private JapanPostalCodeResponse verifySelectedAddress(AdditionalInfoRequest request) {
        List<JapanPostalCodeResponse> candidates = postalCodeSearchService.findByPostalCode(request.getZipCode());
        return candidates.stream()
                .filter(candidate -> candidate.getProvince().equals(normalize(request.getProvince())))
                .filter(candidate -> candidate.getDetailAddress().equals(normalize(request.getBaseAddress())))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("우편번호를 다시 조회해 주소를 선택해주세요."));
    }

    private String joinAddress(String baseAddress, String detailAddress) {
        return normalize(baseAddress) + " " + normalize(detailAddress);
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
