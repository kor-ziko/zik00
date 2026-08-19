package com.zik00.admin.repository.settings_management.common;

import com.zik00.admin.domain.settings_management.common.SettingEntry;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SettingEntryRepository extends JpaRepository<SettingEntry, Long> {
    List<SettingEntry> findByTypeOrderByDisplayOrderAscIdAsc(String type);
    boolean existsByTypeAndCodeAndIdNot(String type, String code, Long id);
}
