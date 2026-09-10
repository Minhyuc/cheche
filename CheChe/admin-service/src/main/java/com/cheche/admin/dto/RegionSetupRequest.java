package com.cheche.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegionSetupRequest(
        @NotBlank @Pattern(regexp = "^[0-9]{2,10}$", message = "법정동/행정구역 코드를 입력해 주세요.") String regionCode,
        @NotBlank String regionName
) {}
