package com.cheche.facility.integration;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KcisaKspoFacilityClient implements KspoFacilityClient {
    private final RestClient restClient;
    private final String apiKey;
    private final int pageSize;

    public KcisaKspoFacilityClient(RestClient.Builder builder,
                                   @Value("${integrations.kspo.base-url}") String baseUrl,
                                   @Value("${integrations.kspo.api-key:}") String apiKey,
                                   @Value("${integrations.kspo.page-size:1000}") int pageSize) {
        this.restClient = builder.baseUrl(baseUrl).build();
        this.apiKey = apiKey;
        this.pageSize = pageSize;
    }

    @Override
    public FetchResult fetchPublicFacilities(String province, String district) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "DATA_GO_KR_SERVICE_KEY가 설정되지 않았습니다.");
        }
        try {
            List<KspoFacilityItem> all = new ArrayList<>();
            int page = 1;
            int totalCount;
            do {
                int pageNumber = page;
                ApiResponse response = restClient.get()
                        .uri(uri -> {
                            var request = uri.queryParam("serviceKey", apiKey)
                                    .queryParam("numOfRows", pageSize)
                                    .queryParam("pageNo", pageNumber)
                                    .queryParam("resultType", "JSON")
                                    .queryParam("faci_gb_nm", "공공")
                                    .queryParam("cp_nm", province);
                            if (district != null && !district.isBlank()) request.queryParam("cpb_nm", district);
                            return request.build();
                        })
                        .accept(MediaType.APPLICATION_JSON)
                        .retrieve()
                        .body(ApiResponse.class);
                if (response == null || response.effectiveBody() == null) {
                    throw new IllegalStateException("공공데이터 응답 본문이 없습니다.");
                }
                ApiHeader header = response.effectiveHeader();
                if (header != null && !("00".equals(header.resultCode()) || "0000".equals(header.resultCode()))) {
                    throw new IllegalStateException("공공데이터 오류: " + header.resultMsg());
                }
                ApiBody body = response.effectiveBody();
                totalCount = parseInt(body.totalCount());
                List<KspoFacilityItem> pageItems = body.items() == null ? null : body.items().item();
                if (pageItems == null || pageItems.isEmpty()) {
                    if (all.size() < totalCount) throw new IllegalStateException("공공데이터 페이지가 비어 있습니다.");
                    break;
                }
                all.addAll(pageItems);
                page++;
            } while (all.size() < totalCount);
            return new FetchResult(totalCount, List.copyOf(all));
        } catch (RestClientException | IllegalStateException exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "국민체육진흥공단 시설정보를 불러오지 못했습니다.", exception);
        }
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException exception) {
            throw new IllegalStateException("공공데이터 전체 건수가 올바르지 않습니다.", exception);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApiResponse(ApiHeader header, ApiBody body, ResponseEnvelope response) {
        ApiHeader effectiveHeader() { return header != null ? header : response == null ? null : response.header(); }
        ApiBody effectiveBody() { return body != null ? body : response == null ? null : response.body(); }
    }
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ResponseEnvelope(ApiHeader header, ApiBody body) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApiHeader(String resultCode, String resultMsg) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApiBody(String pageNo, String totalCount, ApiItems items, String numOfRows) {}
    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ApiItems(
            @JsonFormat(with = JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY)
            List<KspoFacilityItem> item
    ) {}
}
