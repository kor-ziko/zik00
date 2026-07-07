package com.zik00.shop.dto;

import com.zik00.shop.domain.JapanPostalCode;

public class JapanPostalCodeResponse {
    private final Long id;
    private final String zipCode;
    private final String province;
    private final String detailAddress;

    public JapanPostalCodeResponse(JapanPostalCode postalCode) {
        this.id = postalCode.getId();
        this.zipCode = formatPostalCode(postalCode.getPostalCode());
        this.province = postalCode.getPrefecture();
        this.detailAddress = postalCode.getDetailAddress();
    }

    public Long getId() {
        return id;
    }
    public String getZipCode() {
        return zipCode;
    }
    public String getProvince() {
        return province;
    }
    public String getDetailAddress() {
        return detailAddress;
    }

    private String formatPostalCode(String value) {
        if (value == null || value.length() != 7) {
            return value;
        }
        return value.substring(0, 3) + "-" + value.substring(3);
    }
}
