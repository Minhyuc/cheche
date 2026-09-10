package com.cheche.facility.dto;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import java.time.LocalDateTime;

public record FacilityResponse(
        Long id, String name, String type, String regionCode, String regionName,
        String address, String phone, FacilityStatus status, Long managerUserId,
        String publicNotice, LocalDateTime updatedAt
) {
    public static FacilityResponse from(Facility facility) {
        return new FacilityResponse(facility.getId(), facility.getName(), facility.getType(),
                facility.getRegionCode(), facility.getRegionName(), facility.getAddress(),
                facility.getPhone(), facility.getStatus(), facility.getManagerUserId(),
                facility.getPublicNotice(), facility.getUpdatedAt());
    }
}
