package com.cheche.facility.dto;

import java.util.List;

public record UsageGuideResponse(
        Long facilityId,
        String facilityName,
        boolean reservable,
        String reservationChannel,
        String phone,
        String notice,
        List<String> steps
) {
}
