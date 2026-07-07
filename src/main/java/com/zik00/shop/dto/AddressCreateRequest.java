package com.zik00.shop.dto;

public class AddressCreateRequest {
    private String addressName;
    private String receiverName;
    private String receiverPhone;
    private Long postalCodeId;
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

    public Long getPostalCodeId() {
        return postalCodeId;
    }
    public void setPostalCodeId(Long postalCodeId) {
        this.postalCodeId = postalCodeId;
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
