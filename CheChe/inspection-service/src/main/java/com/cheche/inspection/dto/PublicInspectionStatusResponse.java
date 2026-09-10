package com.cheche.inspection.dto;

import com.cheche.inspection.domain.ActionStatus;
import com.cheche.inspection.domain.Inspection;
import java.time.LocalDateTime;

/** 개인정보와 내부 점검 메모를 제외한 시설 이용자 공개용 응답입니다. */
public record PublicInspectionStatusResponse(
        Long id,
        Long facilityId,
        String facilityName,
        String locationDescription,
        String defectType,
        String severity,
        ActionStatus actionStatus,
        String publicActionSummary,
        LocalDateTime createdAt,
        LocalDateTime resolvedAt
) {
    public static PublicInspectionStatusResponse from(Inspection value) {
        String summary = value.getActionStatus() == ActionStatus.RESOLVED
                ? "조치 완료"
                : switch (value.getActionStatus()) {
                    case REPORTED -> "접수 완료";
                    case REVIEWING -> "관리자 확인 중";
                    case ACTION_SCHEDULED -> "조치 예정";
                    case RESOLVED -> "조치 완료";
                };
        return new PublicInspectionStatusResponse(value.getId(), value.getFacilityId(),
                value.getFacilityName(), value.getLocationDescription(), value.getDefectType().name(),
                value.getSeverity().name(), value.getActionStatus(), summary,
                value.getCreatedAt(), value.getResolvedAt());
    }
}
