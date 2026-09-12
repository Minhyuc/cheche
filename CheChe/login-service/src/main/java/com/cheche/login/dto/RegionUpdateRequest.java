package com.cheche.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record RegionUpdateRequest(
        @NotBlank
        @Pattern(regexp = "^11[0-9]{3}$", message = "서울 자치구 코드를 입력해 주세요.")
        String regionCode
) {}
