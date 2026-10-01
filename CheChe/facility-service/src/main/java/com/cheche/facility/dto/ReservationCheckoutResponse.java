package com.cheche.facility.dto;

public record ReservationCheckoutResponse(
        Long facilityId,
        String facilityName,
        int pricePerPerson,
        String reservationMode,
        boolean externalReservationAvailable,
        String externalReservationUrl,
        boolean paymentRequired,
        boolean onlinePaymentAvailable,
        String message
) {}
