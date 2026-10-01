package com.cheche.facility.dto;

import java.time.LocalDate;

public record ReservationDateOption(
        LocalDate date,
        String dayOfWeek,
        String dayLabel,
        boolean available
) {}
