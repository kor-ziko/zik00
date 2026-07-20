package com.zik00.shop.config;

import java.time.LocalDate;

import com.zik00.shop.service.security.PiiEncryptionService;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class PiiLocalDateAttributeConverter implements AttributeConverter<LocalDate, String> {
    private final PiiEncryptionService encryptionService;

    public PiiLocalDateAttributeConverter(PiiEncryptionService encryptionService) {
        this.encryptionService = encryptionService;
    }

    @Override
    public String convertToDatabaseColumn(LocalDate attribute) {
        return attribute == null ? null : encryptionService.encrypt(attribute.toString());
    }

    @Override
    public LocalDate convertToEntityAttribute(String databaseValue) {
        String plaintext = encryptionService.decrypt(databaseValue);
        return plaintext == null || plaintext.isBlank() ? null : LocalDate.parse(plaintext);
    }
}
