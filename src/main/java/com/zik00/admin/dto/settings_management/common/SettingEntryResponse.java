package com.zik00.admin.dto.settings_management.common;

import java.time.LocalDateTime;
import java.util.Map;

public record SettingEntryResponse(Long id, String type, String code, String name, String content,
                                   Map<String,String> fields, int displayOrder, boolean active,
                                   LocalDateTime createdAt, LocalDateTime updatedAt) {}
