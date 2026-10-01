package com.cheche.facility.dto;

import java.time.LocalTime;

public record ReservationTimeSlot(
        LocalTime startTime,
        LocalTime endTime,
        String status,
        String statusLabel,
        int pricePerPerson,
        int capacity,
        int reservedParticipants,
        int remainingCapacity
) {}
