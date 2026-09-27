package com.cheche.facility.dto;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;

public record UserFacilityCard(
        Long id,
        String externalId,
        String source,
        String name,
        String type,
        String regionName,
        String address,
        String phone,
        String imageUrl,
        String openingTime,
        String closingTime,
        FacilityStatus status,
        String statusLabel
) {
    public static UserFacilityCard from(Facility facility) {
        return new UserFacilityCard(facility.getId(), null, "CHECHE", facility.getName(), facility.getType(),
                facility.getRegionName(), facility.getAddress(), facility.getPhone(), null, null, null,
                facility.getStatus(), "운영 중");
    }
}
