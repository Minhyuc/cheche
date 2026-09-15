package com.cheche.inspection.service;

import com.cheche.inspection.dto.FacilitySummary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class FacilityServiceClient {
    private final RestClient client;

    public FacilityServiceClient(RestClient.Builder builder,
                                 @Value("${services.facility.base-url:http://localhost:8083}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public FacilitySummary get(Long facilityId, String regionCode) {
        try {
            FacilitySummary response = client.get()
                    .uri("/api/user/facilities/{id}", facilityId)
                    .header("X-User-Region", regionCode)
                    .retrieve()
                    .body(FacilitySummary.class);
            if (response == null) throw new IllegalStateException("empty facility response");
            return response;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "체육시설 정보를 확인할 수 없습니다.", exception);
        }
    }
}
