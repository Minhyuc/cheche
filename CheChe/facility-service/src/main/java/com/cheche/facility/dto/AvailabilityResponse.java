package com.cheche.facility.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record AvailabilityResponse(
        Long facilityId,
        LocalDate reservationDate,
        List<LocalTime> availableStartTimes
) {}
