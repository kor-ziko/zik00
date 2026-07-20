package com.zik00.shop.service.security;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@ConditionalOnProperty(name = "shop.pii.migrate-plaintext-on-startup", havingValue = "true")
public class PiiPlaintextMigrationRunner implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(PiiPlaintextMigrationRunner.class);

    private final JdbcTemplate jdbcTemplate;
    private final PiiEncryptionService encryptionService;

    public PiiPlaintextMigrationRunner(JdbcTemplate jdbcTemplate, PiiEncryptionService encryptionService) {
        this.jdbcTemplate = jdbcTemplate;
        this.encryptionService = encryptionService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        int migratedUsers = migrateUsers();
        int migratedAddresses = migrateAddresses();
        if (migratedUsers > 0 || migratedAddresses > 0) {
            log.info("Encrypted legacy PII rows: users={}, addresses={}", migratedUsers, migratedAddresses);
        }
    }

    private int migrateUsers() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT user_id, name, name_kana, birth_date, gender, telephone, mobile_phone, email
                FROM `user`
                """);
        List<Object[]> updates = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String name = value(row, "name");
            String nameKana = value(row, "name_kana");
            String birthDate = value(row, "birth_date");
            String gender = value(row, "gender");
            String telephone = value(row, "telephone");
            String mobilePhone = value(row, "mobile_phone");
            String email = value(row, "email");
            if (allCurrentOrEmpty(name, nameKana, birthDate, gender, telephone, mobilePhone, email)) {
                continue;
            }
            updates.add(new Object[]{
                    encryptIfNeeded(name),
                    encryptIfNeeded(nameKana),
                    encryptIfNeeded(birthDate),
                    encryptIfNeeded(gender),
                    encryptIfNeeded(telephone),
                    encryptIfNeeded(mobilePhone),
                    encryptIfNeeded(email),
                    row.get("user_id")
            });
        }
        jdbcTemplate.batchUpdate("""
                UPDATE `user`
                SET name = ?, name_kana = ?, birth_date = ?, gender = ?, telephone = ?, mobile_phone = ?, email = ?
                WHERE user_id = ?
                """, updates);
        return updates.size();
    }

    private int migrateAddresses() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT address_id, address_name, receiver_name, receiver_phone, detail_address
                FROM addresses
                """);
        List<Object[]> updates = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            String addressName = value(row, "address_name");
            String receiverName = value(row, "receiver_name");
            String receiverPhone = value(row, "receiver_phone");
            String detailAddress = value(row, "detail_address");
            if (allCurrentOrEmpty(addressName, receiverName, receiverPhone, detailAddress)) {
                continue;
            }
            updates.add(new Object[]{
                    encryptIfNeeded(addressName),
                    encryptIfNeeded(receiverName),
                    encryptIfNeeded(receiverPhone),
                    encryptIfNeeded(detailAddress),
                    row.get("address_id")
            });
        }
        jdbcTemplate.batchUpdate("""
                UPDATE addresses
                SET address_name = ?, receiver_name = ?, receiver_phone = ?, detail_address = ?
                WHERE address_id = ?
                """, updates);
        return updates.size();
    }

    private String value(Map<String, Object> row, String column) {
        Object value = row.get(column);
        return value == null ? null : value.toString();
    }

    private boolean allCurrentOrEmpty(String... values) {
        for (String value : values) {
            if (value != null && !value.isEmpty() && !encryptionService.isEncryptedWithCurrentKey(value)) {
                return false;
            }
        }
        return true;
    }

    private String encryptIfNeeded(String value) {
        if (value == null || value.isEmpty() || encryptionService.isEncryptedWithCurrentKey(value)) {
            return value;
        }
        String plaintext = encryptionService.isEncrypted(value) ? encryptionService.decrypt(value) : value;
        return encryptionService.encrypt(plaintext);
    }
}
