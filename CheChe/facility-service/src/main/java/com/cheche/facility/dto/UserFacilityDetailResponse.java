package com.cheche.facility.dto;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;

public record UserFacilityDetailResponse(
        Long id,
        String name,
        String type,
        String regionName,
        String address,
        String phone,
        FacilityStatus status,
        String statusLabel,
        String publicNotice,
        boolean reservable,
        String usageGuidePath
) {
    public static UserFacilityDetailResponse from(Facility facility) {
        return new UserFacilityDetailResponse(
                facility.getId(), facility.getName(), facility.getType(), facility.getRegionName(),
                facility.getAddress(), facility.getPhone(), facility.getStatus(),
                statusLabel(facility.getStatus()), facility.getPublicNotice(),
                facility.getStatus() == FacilityStatus.OPERATING,
                "/api/user/facilities/" + facility.getId() + "/usage-guide");
    }

    private static String statusLabel(FacilityStatus status) {
        return switch (status) {
            case OPERATING -> "운영 중";
            case UNDER_INSPECTION -> "점검 중";
            case CLOSED -> "운영 종료";
        };
    }
}
