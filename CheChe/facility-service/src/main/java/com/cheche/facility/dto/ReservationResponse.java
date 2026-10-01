package com.cheche.facility.dto;

import com.cheche.facility.domain.Reservation;
import com.cheche.facility.domain.ReservationStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

public record ReservationResponse(
        Long id,
        Long facilityId,
        String facilityName,
        String regionName,
        LocalDate reservationDate,
        LocalTime startTime,
        LocalTime endTime,
        int participantCount,
        int pricePerPerson,
        int totalFee,
        ReservationStatus status,
        String statusLabel,
        LocalDateTime createdAt,
        LocalDateTime cancelledAt
) {
    public static ReservationResponse from(Reservation value) {
        return new ReservationResponse(value.getId(), value.getFacilityId(), value.getFacilityName(),
                value.getRegionName(), value.getReservationDate(), value.getStartTime(), value.getEndTime(),
                value.getParticipantCount(), value.getPricePerPerson(), value.getTotalFee(),
                value.getStatus(), statusLabel(value.getStatus()),
                value.getCreatedAt(), value.getCancelledAt());
    }

    private static String statusLabel(ReservationStatus status) {
        return switch (status) {
            case CONFIRMED -> "예약 확정";
            case CANCELLED -> "예약 취소";
            case COMPLETED -> "이용 완료";
        };
    }
}
