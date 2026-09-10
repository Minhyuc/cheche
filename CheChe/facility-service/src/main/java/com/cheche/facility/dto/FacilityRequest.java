package com.cheche.facility.dto;

import com.cheche.facility.domain.FacilityStatus;
import jakarta.validation.constraints.NotBlank;

public record FacilityRequest(
        @NotBlank String name,
        @NotBlank String type,
        @NotBlank String regionCode,
        @NotBlank String regionName,
        @NotBlank String address,
        String phone,
        FacilityStatus status,
        String publicNotice
) {}
