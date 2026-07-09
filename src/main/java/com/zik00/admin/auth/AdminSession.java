package com.zik00.admin.auth;

import java.io.Serializable;

public record AdminSession(
        Long adminId,
        String loginId,
        String name
) implements Serializable {
}
