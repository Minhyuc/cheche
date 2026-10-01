package com.cheche.inspection.dto;

public record RegionalSafetySummaryResponse(
        String regionCode,
        String regionName,
        long facilityCount,
        long totalInspections,
        long openInspections,
        long resolvedInspections,
        long highRiskOpenInspections,
        int safetyScore
) {}
