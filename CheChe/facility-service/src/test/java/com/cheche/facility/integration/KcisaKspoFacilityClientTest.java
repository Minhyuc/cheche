package com.cheche.facility.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class KcisaKspoFacilityClientTest {
    @Test
    void parsesOfficialJsonEnvelope() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(queryParam("serviceKey", "test-key"))
                .andExpect(queryParam("numOfRows", "1000"))
                .andExpect(queryParam("pageNo", "1"))
                .andExpect(queryParam("resultType", "JSON"))
                .andExpect(queryParam("faci_gb_nm", "%EA%B3%B5%EA%B3%B5"))
                .andExpect(queryParam("cp_nm", "%EC%84%9C%EC%9A%B8%ED%8A%B9%EB%B3%84%EC%8B%9C"))
                .andExpect(queryParam("cpb_nm", "%EA%B0%95%EB%82%A8%EA%B5%AC"))
                .andExpect(header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE))
                .andRespond(withSuccess("""
                        {"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE"},
                        "body":{"pageNo":"1","totalCount":"1","items":{"item":[{
                        "faci_cd":"FACI-1","faci_nm":"강남수영장","faci_gb_nm":"공공",
                        "faci_road_addr":"서울특별시 강남구 테헤란로 1","faci_tel_no":"02-0000-0000",
                        "faci_homepage":"https://example/1","cp_nm":"서울특별시","cpb_nm":"강남구"}]},"numOfRows":"1000"}}
                        """, MediaType.APPLICATION_JSON));

        KcisaKspoFacilityClient client = new KcisaKspoFacilityClient(
                builder, "https://api.example/facilities", "test-key", 1000);

        KspoFacilityClient.FetchResult result = client.fetchPublicFacilities("서울특별시", "강남구");

        assertEquals(1, result.totalCount());
        assertEquals("강남수영장", result.items().get(0).name());
        server.verify();
    }
}
