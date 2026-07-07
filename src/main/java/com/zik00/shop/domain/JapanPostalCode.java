package com.zik00.shop.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "japan_postal_codes", indexes = {
        @Index(name = "idx_japan_postal_codes_code", columnList = "postal_code")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_japan_postal_codes_address", columnNames = {
                "postal_code", "prefecture", "city", "town"
        })
})
public class JapanPostalCode {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "postal_code", length = 7, nullable = false)
    private String postalCode;

    @Column(nullable = false)
    private String prefecture;

    @Column(nullable = false)
    private String city;

    @Column(nullable = false)
    private String town;

    protected JapanPostalCode() {
    }

    public JapanPostalCode(String postalCode, String prefecture, String city, String town) {
        this.postalCode = postalCode;
        this.prefecture = prefecture;
        this.city = city;
        this.town = town;
    }

    public String getPostalCode() {
        return postalCode;
    }
    public Long getId() {
        return id;
    }
    public String getPrefecture() {
        return prefecture;
    }
    public String getCity() {
        return city;
    }
    public String getTown() {
        return town;
    }
    public String getDetailAddress() {
        return city + town;
    }
}
