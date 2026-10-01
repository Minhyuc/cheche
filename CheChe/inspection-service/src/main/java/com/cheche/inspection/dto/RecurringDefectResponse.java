package com.cheche.inspection.dto;

import com.cheche.inspection.domain.DefectType;
import com.cheche.inspection.domain.Severity;
import java.time.LocalDateTime;

public record RecurringDefectResponse(
        Long facilityId,
        String facilityName,
        String regionCode,
        String regionName,
        DefectType defectType,
        long occurrenceCount,
        long openCount,
        Severity highestSeverity,
        LocalDateTime lastDetectedAt
) {}
