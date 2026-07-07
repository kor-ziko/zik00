package com.zik00.shop.dto;

public class ProfileUpdateRequest {
    private String name;
    private String nickname;
    private String zipCode;
    private String province;
    private String detailAddress;
    private String mobilePhone;
    private String email;
    private boolean alarmConsent;

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    public String getNickname() {
        return nickname;
    }
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getZipCode() {
        return zipCode;
    }
    public void setZipCode(String zipCode) {
        this.zipCode = zipCode;
    }

    public String getProvince() {
        return province;
    }
    public void setProvince(String province) {
        this.province = province;
    }

    public String getDetailAddress() {
        return detailAddress;
    }
    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public String getMobilePhone() {
        return mobilePhone;
    }
    public void setMobilePhone(String mobilePhone) {
        this.mobilePhone = mobilePhone;
    }

    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }

    public boolean isAlarmConsent() {
        return alarmConsent;
    }
    public void setAlarmConsent(boolean alarmConsent) {
        this.alarmConsent = alarmConsent;
    }
}
