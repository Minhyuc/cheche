package com.cheche.facility.dto;

public record AiSearchConditions(
        String region,
        String sport,
        String time,
        boolean reservationAvailableOnly
) {}
