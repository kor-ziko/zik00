package com.zik00.admin.dto;

import com.zik00.shop.domain.DeliveryAddress;

// @dev - 관리자 로그인 성공/실패 처리
public record AdminMemberAddressResponse(
        Long id,
        String receiverName,
        String phone,
        String postalCode,
        String address1,
        String address2,
        String countryCode,
        boolean isDefault
) {

    public static AdminMemberAddressResponse from(DeliveryAddress address) {
        return new AdminMemberAddressResponse(
                address.getId(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getZipCode(),
                joinAddress(address.getProvince(), address.getDetailAddress()),
                address.getAddressName(),
                "JP",
                address.isDefaultAddress()
        );
    }

    private static String joinAddress(String province, String detailAddress) {
        String normalizedProvince = normalize(province);
        String normalizedDetailAddress = normalize(detailAddress);

        if (normalizedProvince.isBlank()) {
            return normalizedDetailAddress;
        }
        if (normalizedDetailAddress.isBlank()) {
            return normalizedProvince;
        }
        return normalizedProvince + " " + normalizedDetailAddress;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
