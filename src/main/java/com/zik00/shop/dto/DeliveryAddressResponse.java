package com.zik00.shop.dto;

import com.zik00.shop.domain.DeliveryAddress;
import lombok.Getter;

@Getter
public class DeliveryAddressResponse {
    private final long id;
    private final String addressName;
    private final String receiverName;
    private final String receiverPhone;
    private final String zipCode;
    private final String province;
    private final String detailAddress;
    private final boolean defaultAddress;

    private DeliveryAddressResponse(
            long id,
            String addressName,
            String receiverName,
            String receiverPhone,
            String zipCode,
            String province,
            String detailAddress,
            boolean defaultAddress
    ) {
        this.id = id;
        this.addressName = addressName;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.province = province;
        this.detailAddress = detailAddress;
        this.defaultAddress = defaultAddress;
    }

    public static DeliveryAddressResponse from(DeliveryAddress address) {
        return new DeliveryAddressResponse(
                address.getId(),
                address.getAddressName(),
                address.getReceiverName(),
                address.getReceiverPhone(),
                address.getZipCode(),
                address.getProvince(),
                address.getDetailAddress(),
                address.isDefaultAddress()
        );
    }
}
