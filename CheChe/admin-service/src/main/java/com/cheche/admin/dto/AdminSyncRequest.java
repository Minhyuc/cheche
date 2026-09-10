package com.cheche.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AdminSyncRequest(
        @NotNull Long userId,
        @NotBlank String username
) {}
