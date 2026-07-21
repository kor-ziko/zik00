package com.zik00.shop.service.auth;

import java.util.List;

import com.zik00.shop.domain.DeliveryAddress;
import com.zik00.shop.domain.User;
import com.zik00.shop.dto.auth.RegistrationDetailRequest;
import com.zik00.shop.dto.mypage.JapanPostalCodeResponse;
import com.zik00.shop.repository.DeliveryAddressRepository;
import com.zik00.shop.repository.UserRepository;
import com.zik00.shop.service.JapanPostalCodeSearchService;
import com.zik00.shop.util.PhoneNumberFormatter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RegistrationService {
    private final AuthenticatedUserService authenticatedUserService;
    private final UserRepository userRepository;
    private final DeliveryAddressRepository deliveryAddressRepository;
    private final JapanPostalCodeSearchService postalCodeSearchService;

    public RegistrationService(
            AuthenticatedUserService authenticatedUserService,
            UserRepository userRepository,
            DeliveryAddressRepository deliveryAddressRepository,
            JapanPostalCodeSearchService postalCodeSearchService
    ) {
        this.authenticatedUserService = authenticatedUserService;
        this.userRepository = userRepository;
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

    public PreparedRegistration prepareRegistration(RegistrationDetailRequest request) {
        JapanPostalCodeResponse selectedAddress = verifySelectedAddress(request);
        String telephone = PhoneNumberFormatter.formatTelephone(request.getTelephone());
        String mobilePhone = PhoneNumberFormatter.formatMobilePhone(request.getMobilePhone());
        return new PreparedRegistration(
                request.getNameKanji(),
                request.getNameKatakana(),
                request.getBirthDate(),
                request.getGender(),
                request.getNickname(),
                telephone,
                mobilePhone,
                selectedAddress.getZipCode(),
                selectedAddress.getProvince(),
                joinAddress(selectedAddress.getDetailAddress(), request.getDetailAddress())
        );
    }

    @Transactional
    public User completeOAuthRegistration(
            PendingOAuthRegistrationService.AcceptedOAuthRegistration acceptedRegistration,
            PreparedRegistration detail
    ) {
        OAuthProfile oauthAccount = acceptedRegistration.account();
        String loginId = oauthAccount.loginId();
        User user = userRepository.findByLoginId(loginId)
                .orElseGet(() -> User.createOAuthUser(
                        loginId,
                        oauthAccount.email()
                ));
        boolean hasDeliveryAddress = user.getMemberId() > 0
                && deliveryAddressRepository.existsByMemberId(user.getMemberId());
        if (user.hasCompletedRegistration() && hasDeliveryAddress) {
            return user;
        }

        user.completeRegistration(
                detail.nameKanji(),
                detail.nameKatakana(),
                detail.birthDate(),
                detail.gender(),
                detail.nickname(),
                detail.telephone(),
                detail.mobilePhone(),
                acceptedRegistration.alarmConsent()
        );
        user = userRepository.save(user);

        if (!hasDeliveryAddress) {
            deliveryAddressRepository.save(new DeliveryAddress(
                    0,
                    user.getMemberId(),
                    "기본 배송지",
                    detail.nameKanji(),
                    detail.mobilePhone(),
                    detail.zipCode(),
                    detail.province(),
                    detail.detailAddress(),
                    true
            ));
        }
        return user;
    }

    private JapanPostalCodeResponse verifySelectedAddress(RegistrationDetailRequest request) {
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

    public record PreparedRegistration(
            String nameKanji,
            String nameKatakana,
            java.time.LocalDate birthDate,
            String gender,
            String nickname,
            String telephone,
            String mobilePhone,
            String zipCode,
            String province,
            String detailAddress
    ) {
    }
}
