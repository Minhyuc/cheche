package com.cheche.facility.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record NaturalLanguageSearchRequest(
        @NotBlank @Size(max = 200) String query
) {
}
