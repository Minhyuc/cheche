package com.cheche.facility.dto;

import java.util.List;

public record UserHomeResponse(
        String title,
        String description,
        String regionCode,
        String aiExamplePrompt,
        List<String> quickSports,
        List<UserFacilityCard> recommendations,
        List<UserFacilityCard> kspoFacilities
) {
}
