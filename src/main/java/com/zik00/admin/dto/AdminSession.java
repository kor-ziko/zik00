package com.zik00.admin.dto;

import java.io.Serializable;

public record AdminSession(
        Long adminId,
        String loginId,
        String name
) implements Serializable {
}
