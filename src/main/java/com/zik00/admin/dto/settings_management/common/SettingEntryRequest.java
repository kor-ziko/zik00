package com.zik00.admin.dto.settings_management.common;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.Map;

public record SettingEntryRequest(
        @NotBlank @Size(max = 100) String code,
        @NotBlank @Size(max = 200) String name,
        @Size(max = 50_000) String content,
        @NotNull Map<String, String> fields,
        @Min(0) int displayOrder,
        boolean active
) {}
