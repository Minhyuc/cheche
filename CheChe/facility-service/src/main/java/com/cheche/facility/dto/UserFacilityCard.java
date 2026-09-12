package com.cheche.facility.dto;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;

public record UserFacilityCard(
        Long id,
        String name,
        String type,
        String regionName,
        String address,
        FacilityStatus status,
        String statusLabel
) {
    public static UserFacilityCard from(Facility facility) {
        return new UserFacilityCard(facility.getId(), facility.getName(), facility.getType(),
                facility.getRegionName(), facility.getAddress(), facility.getStatus(), "운영 중");
    }
}
