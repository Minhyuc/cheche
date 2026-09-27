package com.cheche.facility.service;

import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.UserFacilityCard;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class SeoulPublicFacilityClient {
    private static final Logger log = LoggerFactory.getLogger(SeoulPublicFacilityClient.class);
    private static final Map<String, String> DISTRICTS = Map.ofEntries(
            Map.entry("11110", "종로구"), Map.entry("11140", "중구"), Map.entry("11170", "용산구"),
            Map.entry("11200", "성동구"), Map.entry("11215", "광진구"), Map.entry("11230", "동대문구"),
            Map.entry("11260", "중랑구"), Map.entry("11290", "성북구"), Map.entry("11305", "강북구"),
            Map.entry("11320", "도봉구"), Map.entry("11350", "노원구"), Map.entry("11380", "은평구"),
            Map.entry("11410", "서대문구"), Map.entry("11440", "마포구"), Map.entry("11470", "양천구"),
            Map.entry("11500", "강서구"), Map.entry("11530", "구로구"), Map.entry("11545", "금천구"),
            Map.entry("11560", "영등포구"), Map.entry("11590", "동작구"), Map.entry("11620", "관악구"),
            Map.entry("11650", "서초구"), Map.entry("11680", "강남구"), Map.entry("11710", "송파구"),
            Map.entry("11740", "강동구"));

    private final RestClient client;
    private final String apiKey;
    private final int fetchLimit;

    public SeoulPublicFacilityClient(
            RestClient.Builder builder,
            @Value("${seoul.open-api.base-url:http://openapi.seoul.go.kr:8088}") String baseUrl,
            @Value("${seoul.open-api.key:sample}") String apiKey,
            @Value("${seoul.open-api.fetch-limit:1000}") int fetchLimit) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.fetchLimit = fetchLimit;
    }

    public List<UserFacilityCard> findByRegion(String regionCode) {
        String district = DISTRICTS.get(regionCode);
        if (district == null) return List.of();
        try {
            JsonNode response = client.get()
                    .uri("/{key}/json/ListPublicReservationSport/1/{limit}/", apiKey, fetchLimit)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode rows = response == null ? null : response.path("ListPublicReservationSport").path("row");
            if (rows == null || !rows.isArray()) return List.of();

            Map<String, UserFacilityCard> unique = new LinkedHashMap<>();
            for (JsonNode row : rows) {
                if (!district.equals(text(row, "AREANM"))) continue;
                String placeName = firstNonBlank(text(row, "PLACENM"), text(row, "SVCNM"));
                String type = firstNonBlank(text(row, "MINCLASSNM"), "체육시설");
                String deduplicationKey = district + '|' + placeName + '|' + type;
                unique.putIfAbsent(deduplicationKey, new UserFacilityCard(
                        null,
                        text(row, "SVCID"),
                        "SEOUL_OPEN_API",
                        placeName,
                        type,
                        "서울특별시 " + district,
                        placeName,
                        text(row, "TELNO"),
                        text(row, "IMGURL"),
                        text(row, "V_MIN"),
                        text(row, "V_MAX"),
                        FacilityStatus.OPERATING,
                        firstNonBlank(text(row, "SVCSTATNM"), "운영 정보 제공")));
            }
            return new ArrayList<>(unique.values());
        } catch (Exception exception) {
            log.warn("서울시 체육시설 API 조회에 실패했습니다. 기존 DB 검색 결과만 반환합니다: {}",
                    exception.getMessage());
            return List.of();
        }
    }

    private String text(JsonNode row, String field) {
        String value = row.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private String firstNonBlank(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }
}
