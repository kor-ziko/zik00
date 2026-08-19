package com.zik00.admin.repository.settings_management.mail_management;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MailTemplateRepository extends JpaRepository<SettingEntry, Long> {
    List<SettingEntry> findByTypeInOrderByDisplayOrderAscIdAsc(Collection<String> types);
}
