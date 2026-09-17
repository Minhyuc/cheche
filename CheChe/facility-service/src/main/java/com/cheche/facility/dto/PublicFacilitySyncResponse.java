package com.cheche.facility.dto;

public record PublicFacilitySyncResponse(
        String provider,
        String regionCode,
        String regionName,
        int scannedCount,
        int matchedCount,
        int createdCount,
        int updatedCount
) {}
