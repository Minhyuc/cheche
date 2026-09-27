package com.cheche.facility.dto;

import java.util.List;

public record UserHomeResponse(
        String title,
        String description,
        String regionCode,
        List<UserFacilityCard> recommendations,
        List<UserFacilityCard> kspoFacilities
) {
}
