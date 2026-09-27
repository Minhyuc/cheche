package com.cheche.facility.service;

import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.UserFacilityCard;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/** 국민체육진흥공단 스포츠가치센터 운영시설 이용회차 OpenAPI 연동. */
@Component
public class KspoSportsValueClient {
    private static final Logger log = LoggerFactory.getLogger(KspoSportsValueClient.class);
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("H:mm");

    private final RestClient client;
    private final String apiKey;
    private final int fetchLimit;

    public KspoSportsValueClient(
            RestClient.Builder builder,
            @Value("${kspo.open-api.base-url:https://apis.data.go.kr/B551014/SRVC_VCENTER_TMTBL}") String baseUrl,
            @Value("${kspo.open-api.key:}") String apiKey,
            @Value("${kspo.open-api.fetch-limit:1000}") int fetchLimit) {
        this.client = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.fetchLimit = Math.min(Math.max(fetchLimit, 1), 1000);
    }

    public List<UserFacilityCard> findAll() {
        if (apiKey.isBlank()) return List.of();
        try {
            JsonNode root = client.get()
                    .uri(uri -> uri.path("/FCLT_OPRTN_TMTBL")
                            .queryParam("serviceKey", apiKey)
                            .queryParam("pageNo", 1)
                            .queryParam("numOfRows", fetchLimit)
                            .queryParam("resultType", "json")
                            .build())
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode response = root == null ? null : root.path("response");
            String resultCode = response == null ? null : response.path("header").path("resultCode").asText();
            if (response == null || !("00".equals(resultCode) || "0".equals(resultCode))) {
                String message = response == null ? "응답 없음" : response.path("header").path("resultMsg").asText();
                log.warn("KSPO 운영시설 API가 정상 응답을 반환하지 않았습니다: {} {}", resultCode, message);
                return List.of();
            }

            JsonNode item = response.path("body").path("items").path("item");
            List<JsonNode> rows = new ArrayList<>();
            if (item.isArray()) item.forEach(rows::add);
            else if (item.isObject()) rows.add(item);

            Map<String, FacilitySchedule> schedules = new LinkedHashMap<>();
            for (JsonNode row : rows) {
                String facilityId = text(row, "fclt_id");
                String facilityName = text(row, "fclt_nm");
                if (facilityId == null || facilityName == null) continue;
                schedules.computeIfAbsent(facilityId, ignored -> new FacilitySchedule(facilityId, facilityName))
                        .add(row);
            }
            return schedules.values().stream().map(FacilitySchedule::toCard).toList();
        } catch (Exception exception) {
            log.warn("KSPO 운영시설 API 조회에 실패했습니다. 기존 시설 검색 결과만 반환합니다: {}",
                    exception.getMessage());
            return List.of();
        }
    }

    private static String text(JsonNode row, String field) {
        String value = row.path(field).asText("").trim();
        return value.isEmpty() ? null : value;
    }

    private static LocalTime time(String value) {
        if (value == null) return null;
        try {
            String normalized = value.trim().replace('.', ':');
            if (normalized.matches("\\d{4}")) normalized = normalized.substring(0, 2) + ":" + normalized.substring(2);
            return LocalTime.parse(normalized, TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            return null;
        }
    }

    private static String firstNonBlank(String first, String second) {
        return first == null || first.isBlank() ? second : first;
    }

    private static final class FacilitySchedule {
        private final String id;
        private final String name;
        private String building;
        private String floor;
        private LocalTime opening;
        private LocalTime closing;
        private int maximumCapacity;

        private FacilitySchedule(String id, String name) {
            this.id = id;
            this.name = name;
        }

        private void add(JsonNode row) {
            building = firstNonBlank(building, text(row, "fclt_bldg_nm"));
            floor = firstNonBlank(floor, text(row, "fclt_flr"));
            LocalTime start = time(text(row, "bgng_hr_nm"));
            LocalTime end = time(text(row, "end_hr_nm"));
            if (start != null && (opening == null || start.isBefore(opening))) opening = start;
            if (end != null && (closing == null || end.isAfter(closing))) closing = end;
            try {
                maximumCapacity = Math.max(maximumCapacity, Integer.parseInt(text(row, "oprtn_psncpa")));
            } catch (NumberFormatException | NullPointerException ignored) {
                // 수용 인원이 누락되어도 시설과 운영시간은 표시합니다.
            }
        }

        private UserFacilityCard toCard() {
            String location = String.join(" ", java.util.stream.Stream.of(building, floor)
                    .filter(value -> value != null && !value.isBlank()).toList());
            String status = maximumCapacity > 0 ? "KSPO 공식 · 회차 최대 " + maximumCapacity + "명" : "KSPO 공식 운영시설";
            return new UserFacilityCard(null, id, "KSPO_OPEN_API", name,
                    firstNonBlank(building, "스포츠가치센터"), "KSPO 스포츠가치센터",
                    location.isBlank() ? "스포츠가치센터" : location, null, null,
                    opening == null ? null : opening.toString(), closing == null ? null : closing.toString(),
                    FacilityStatus.OPERATING, status);
        }
    }
}
