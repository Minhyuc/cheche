package com.cheche.inspection.dto;

import com.cheche.inspection.domain.*;
import java.time.LocalDateTime;

public record FacilityReportResponse(
        Long id,
        Long facilityId,
        String facilityName,
        String regionCode,
        String regionName,
        ReportCategory category,
        String categoryLabel,
        String locationDescription,
        String comment,
        String photoUrl,
        ReportStatus status,
        String statusLabel,
        String resolutionNote,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static FacilityReportResponse from(FacilityReport report) {
        return new FacilityReportResponse(report.getId(), report.getFacilityId(), report.getFacilityName(),
                report.getRegionCode(), report.getRegionName(), report.getCategory(),
                categoryLabel(report.getCategory()), report.getLocationDescription(), report.getComment(),
                report.getPhotoUrl(), report.getStatus(), statusLabel(report.getStatus()),
                report.getResolutionNote(), report.getCreatedAt(), report.getUpdatedAt());
    }

    private static String categoryLabel(ReportCategory category) {
        return switch (category) {
            case DETERIORATION -> "시설 노후";
            case IMPROVEMENT -> "개선 요청";
            case REPAIR -> "수리 필요";
            case OTHER -> "기타";
        };
    }

    private static String statusLabel(ReportStatus status) {
        return switch (status) {
            case RECEIVED -> "접수 완료";
            case REVIEWING -> "검토 중";
            case REPAIR_SCHEDULED -> "조치 예정";
            case COMPLETED -> "처리 완료";
            case REJECTED -> "처리 대상 아님";
        };
    }
}
