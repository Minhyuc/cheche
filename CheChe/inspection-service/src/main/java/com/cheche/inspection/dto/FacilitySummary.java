package com.cheche.inspection.dto;

public record FacilitySummary(
        Long id,
        String name,
        String type,
        String regionName,
        String address,
        String phone,
        String status,
        String statusLabel,
        String publicNotice,
        boolean reservable,
        String usageGuidePath
) {}
