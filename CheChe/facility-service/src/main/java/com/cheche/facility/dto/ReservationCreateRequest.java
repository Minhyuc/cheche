package com.cheche.facility.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.LocalTime;

public record ReservationCreateRequest(
        @NotNull Long facilityId,
        @NotNull @FutureOrPresent LocalDate reservationDate,
        @NotNull LocalTime startTime,
        @Min(1) @Max(20) int participantCount
) {}
