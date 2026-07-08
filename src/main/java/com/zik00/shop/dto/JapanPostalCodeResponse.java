package com.zik00.shop.dto;

public class JapanPostalCodeResponse {
    private final String zipCode;
    private final String province;
    private final String detailAddress;

    public JapanPostalCodeResponse(String zipCode, String province, String detailAddress) {
        this.zipCode = formatPostalCode(zipCode);
        this.province = normalize(province);
        this.detailAddress = normalize(detailAddress);
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

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
}
