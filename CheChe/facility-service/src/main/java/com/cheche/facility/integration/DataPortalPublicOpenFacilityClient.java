package com.cheche.facility.integration;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class DataPortalPublicOpenFacilityClient implements PublicOpenFacilityClient {
    private static final String DATASET_ID = "15013117";
    private static final int PAGE_SIZE = 10_000;
    private final String baseUrl;
    private final ObjectMapper objectMapper;

    public DataPortalPublicOpenFacilityClient(
            @Value("${public-facility.portal.base-url:https://www.data.go.kr}") String baseUrl,
            ObjectMapper objectMapper) {
        this.baseUrl = baseUrl;
        this.objectMapper = objectMapper;
    }

    @Override
    public FetchResult fetchAll() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        HttpClient client = HttpClient.newBuilder().cookieHandler(cookies)
                .connectTimeout(Duration.ofSeconds(10)).build();
        try {
            send(client, "/data/" + DATASET_ID + "/standard.do");
            String headerJson = send(client, "/download/columList.json?pk=" + DATASET_ID + "&ext=JSON");
            JsonNode header = objectMapper.readTree(headerJson);
            int totalCount = header.path("totalCount").asInt();
            String table = header.path("tableVO").path("svcTableNm").asText();
            List<String> columns = new ArrayList<>();
            header.path("tableVO").path("colNmList").forEach(value -> columns.add(value.asText()));
            if (totalCount <= 0 || table.isBlank() || columns.isEmpty()) {
                throw new IllegalStateException("공공시설 표준데이터 메타정보가 올바르지 않습니다.");
            }

            List<PublicOpenFacilityItem> items = new ArrayList<>();
            int pages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
            for (int page = 1; page <= pages; page++) {
                StringBuilder query = new StringBuilder("/download/standard.json?publicDataPk=")
                        .append(DATASET_ID).append("&svcTableNm=").append(encode(table))
                        .append("&totalCount=").append(totalCount)
                        .append("&perPage=").append(PAGE_SIZE).append("&page=").append(page);
                columns.forEach(column -> query.append("&colNmList=").append(encode(column)));
                items.addAll(objectMapper.readValue(send(client, query.toString()), new TypeReference<>() {}));
            }
            return new FetchResult(totalCount, List.copyOf(items));
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw unavailable(exception);
        } catch (Exception exception) {
            throw unavailable(exception);
        }
    }

    private String send(HttpClient client, String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .timeout(Duration.ofSeconds(30))
                .header("Accept", "application/json,text/html;q=0.9")
                .header("Referer", baseUrl + "/data/" + DATASET_ID + "/standard.do")
                .header("X-Requested-With", "XMLHttpRequest")
                .GET().build();
        HttpResponse<String> response = client.send(request,
                HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        if (response.statusCode() >= 400) {
            throw new IllegalStateException("공공데이터포털 응답 코드: " + response.statusCode());
        }
        return response.body();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private ResponseStatusException unavailable(Exception cause) {
        return new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "전국공공시설개방정보를 불러오지 못했습니다.", cause);
    }
}
