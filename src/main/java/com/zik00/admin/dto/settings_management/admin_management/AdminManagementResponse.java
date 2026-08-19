package com.zik00.admin.dto.settings_management.admin_management;

import com.zik00.admin.domain.AdminUser;
import java.time.LocalDateTime;

public record AdminManagementResponse(Long id,String loginId,String name,boolean active,LocalDateTime createdAt){
    public static AdminManagementResponse from(AdminUser user){return new AdminManagementResponse(user.getAdminId(),user.getLoginId(),user.getName(),user.isActive(),user.getCreatedAt());}
}
