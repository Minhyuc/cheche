package com.cheche.inspection.dto;

import com.cheche.inspection.domain.ActionStatus;
import jakarta.validation.constraints.NotNull;

public record ActionUpdateRequest(@NotNull ActionStatus status, String actionNote) {}
