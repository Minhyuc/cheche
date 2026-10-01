package com.cheche.facility.dto;

import java.time.LocalDate;
import java.util.List;

public record ReservationOptionsResponse(
        Long facilityId,
        String facilityName,
        String facilityType,
        LocalDate selectedDate,
        int pricePerPerson,
        int minParticipants,
        int maxParticipants,
        List<ReservationDateOption> dates,
        List<ReservationTimeSlot> timeSlots
) {}
