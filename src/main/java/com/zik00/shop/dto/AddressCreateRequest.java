package com.zik00.shop.dto;

public class AddressCreateRequest {
    private String addressName;
    private String receiverName;
    private String receiverPhone;
    private String zipCode;
    private String province;
    private String baseAddress;
    private String detailAddress;
    private boolean defaultAddress;

    public String getAddressName() {
        return addressName;
    }
    public void setAddressName(String addressName) {
        this.addressName = addressName;
    }

    public String getReceiverName() {
        return receiverName;
    }
    public void setReceiverName(String receiverName) {
        this.receiverName = receiverName;
    }

    public String getReceiverPhone() {
        return receiverPhone;
    }
    public void setReceiverPhone(String receiverPhone) {
        this.receiverPhone = receiverPhone;
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

    public String getBaseAddress() {
        return baseAddress;
    }
    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }

    public String getDetailAddress() {
        return detailAddress;
    }
    public void setDetailAddress(String detailAddress) {
        this.detailAddress = detailAddress;
    }

    public boolean isDefaultAddress() {
        return defaultAddress;
    }
    public void setDefaultAddress(boolean defaultAddress) {
        this.defaultAddress = defaultAddress;
    }
}
