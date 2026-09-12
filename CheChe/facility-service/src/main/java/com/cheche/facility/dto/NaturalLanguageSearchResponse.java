package com.cheche.facility.dto;

import java.util.List;

public record NaturalLanguageSearchResponse(
        String query,
        int totalCount,
        boolean empty,
        String message,
        List<String> suggestions,
        List<UserFacilityCard> facilities
) {
}
