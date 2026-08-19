package com.zik00.admin.repository.settings_management.mail_address_management;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailAddressRepository extends JpaRepository<SettingEntry, Long> {
    List<SettingEntry> findByTypeOrderByIdAsc(String type);
}
