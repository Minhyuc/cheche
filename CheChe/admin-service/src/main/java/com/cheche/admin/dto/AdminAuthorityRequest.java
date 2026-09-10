package com.cheche.admin.dto;

import com.cheche.admin.domain.AdminRole;
import com.cheche.admin.domain.AdminStatus;
import jakarta.validation.constraints.NotNull;

public record AdminAuthorityRequest(
        @NotNull AdminRole role,
        @NotNull AdminStatus status,
        String regionCode,
        String regionName
) {}
