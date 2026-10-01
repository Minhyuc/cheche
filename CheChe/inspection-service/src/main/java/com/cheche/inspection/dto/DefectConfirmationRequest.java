package com.cheche.inspection.dto;

import com.cheche.inspection.domain.DefectType;
import com.cheche.inspection.domain.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record DefectConfirmationRequest(
        @NotNull DefectType defectType,
        @NotNull Severity severity,
        @NotBlank @Size(max = 240) String locationDescription,
        @Size(max = 1200) String detail,
        @NotNull Boolean actionRequired,
        LocalDate actionDueDate
) {}
