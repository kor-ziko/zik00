package com.zik00.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;

@Getter
@Entity
@Table(name = "addresses", indexes = {
        @Index(name = "idx_addresses_member_default", columnList = "user_id, default_address, address_id")
})
public class DeliveryAddress {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "address_id")
    private Long id;

    @Column(name = "user_id")
    private long memberId;

    @Column(name = "address_name")
    private String addressName;

    @Column(name = "receiver_name")
    private String receiverName;

    @Column(name = "receiver_phone")
    private String receiverPhone;

    @Column(name = "zip_code")
    private String zipCode;

    private String province;

    @Column(name = "detail_address")
    private String detailAddress;

    @Column(name = "default_address")
    private boolean defaultAddress;

    protected DeliveryAddress() {
    }

    public DeliveryAddress(
            long id,
            long memberId,
            String addressName,
            String receiverName,
            String receiverPhone,
            String zipCode,
            String province,
            String detailAddress,
            boolean defaultAddress
    ) {
        this.id = id > 0 ? id : null;
        this.memberId = memberId;
        this.addressName = addressName;
        this.receiverName = receiverName;
        this.receiverPhone = receiverPhone;
        this.zipCode = zipCode;
        this.province = province;
        this.detailAddress = detailAddress;
        this.defaultAddress = defaultAddress;
    }

    public void update(
            String addressName,
            String receiverName,
            String receiverPhone,
            String zipCode,
            String province,
            String detailAddress,
            boolean defaultAddress
    ) {
        this.addressName = normalize(addressName);
        this.receiverName = normalize(receiverName);
        this.receiverPhone = normalize(receiverPhone);
        this.zipCode = normalize(zipCode);
        this.province = normalize(province);
        this.detailAddress = normalize(detailAddress);
        this.defaultAddress = defaultAddress;
    }

    public void clearDefaultAddress() {
        this.defaultAddress = false;
    }
    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }
    public long getId() {
        return id == null ? 0L : id;
    }
}
