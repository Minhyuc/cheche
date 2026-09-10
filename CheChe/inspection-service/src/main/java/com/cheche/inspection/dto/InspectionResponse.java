package com.cheche.inspection.dto;

import com.cheche.inspection.domain.*;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

public record InspectionResponse(
        Long id, Long facilityId, String facilityName, String regionCode, String regionName,
        Long reporterUserId, String photoUrl, String locationDescription, String note,
        DefectType defectType, Severity severity, double confidence, List<String> checklist,
        List<String> similarCases, String reportSummary, ActionStatus actionStatus,
        String actionNote, LocalDateTime resolvedAt, LocalDateTime createdAt, LocalDateTime updatedAt
) {
    public static InspectionResponse from(Inspection value) {
        return new InspectionResponse(value.getId(), value.getFacilityId(), value.getFacilityName(),
                value.getRegionCode(), value.getRegionName(), value.getReporterUserId(), value.getPhotoUrl(),
                value.getLocationDescription(), value.getNote(), value.getDefectType(), value.getSeverity(),
                value.getConfidence(), lines(value.getChecklist()), lines(value.getSimilarCases()),
                value.getReportSummary(), value.getActionStatus(), value.getActionNote(), value.getResolvedAt(),
                value.getCreatedAt(), value.getUpdatedAt());
    }

    private static List<String> lines(String value) {
        return value == null || value.isBlank() ? List.of() : Arrays.asList(value.split("\\n"));
    }
}
