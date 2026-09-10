package com.cheche.admin.dto;

import com.cheche.admin.domain.AdminRole;
import com.cheche.admin.domain.AdminStatus;
import com.cheche.admin.domain.Administrator;
import java.time.LocalDateTime;

public record AdminResponse(
        Long id,
        Long userId,
        String name,
        String email,
        AdminRole role,
        AdminStatus status,
        String regionCode,
        String regionName,
        boolean initialSetupRequired,
        LocalDateTime createdAt
) {
    public static AdminResponse from(Administrator admin) {
        boolean needsSetup = admin.getRole() == AdminRole.REGIONAL_ADMIN
                && (admin.getRegionCode() == null || admin.getRegionCode().isBlank());
        return new AdminResponse(admin.getId(), admin.getUserId(), admin.getName(), admin.getEmail(),
                admin.getRole(), admin.getStatus(), admin.getRegionCode(), admin.getRegionName(),
                needsSetup, admin.getCreatedAt());
    }
}
