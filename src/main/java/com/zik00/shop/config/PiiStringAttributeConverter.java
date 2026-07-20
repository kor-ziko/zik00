package com.zik00.shop.config;

import com.zik00.shop.service.security.PiiEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PiiStringAttributeConverter implements AttributeConverter<String, String> {
    private final PiiEncryptionService encryptionService;

    public PiiStringAttributeConverter(PiiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(String attribute) {
        return encryptionService.encrypt(attribute);
    }

    @Override
    public String convertToEntityAttribute(String databaseValue) {
        return encryptionService.decrypt(databaseValue);
    }
}
