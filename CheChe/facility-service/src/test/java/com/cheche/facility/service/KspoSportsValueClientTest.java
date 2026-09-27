package com.cheche.facility.service;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.springframework.http.MediaType;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.test.web.client.MockRestServiceServer;

class KspoSportsValueClientTest {
    @Test
    void missingKeyReturnsEmptyListWithoutCallingExternalApi() {
        KspoSportsValueClient client = new KspoSportsValueClient(
                RestClient.builder(), "https://example.test", "", 1000);

        assertTrue(client.findAll().isEmpty());
    }

    @Test
    void groupsKspoTimetableRowsByFacility() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        server.expect(requestTo("https://example.test/FCLT_OPRTN_TMTBL?serviceKey=test-key&pageNo=1&numOfRows=1000&resultType=json"))
                .andRespond(withSuccess("""
                        {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE"},
                        "body":{"items":{"item":[
                        {"fclt_id":"F1","fclt_nm":"다목적체육관","fclt_bldg_nm":"본관","fclt_flr":"1층","bgng_hr_nm":"09:00","end_hr_nm":"10:00","oprtn_psncpa":"20"},
                        {"fclt_id":"F1","fclt_nm":"다목적체육관","fclt_bldg_nm":"본관","fclt_flr":"1층","bgng_hr_nm":"18:00","end_hr_nm":"20:00","oprtn_psncpa":"30"}
                        ]}}}}
                        """, MediaType.APPLICATION_JSON));
        KspoSportsValueClient client = new KspoSportsValueClient(builder, "https://example.test", "test-key", 1000);

        var facilities = client.findAll();

        assertEquals(1, facilities.size());
        assertEquals("09:00", facilities.get(0).openingTime());
        assertEquals("20:00", facilities.get(0).closingTime());
        assertEquals("KSPO 공식 · 회차 최대 30명", facilities.get(0).statusLabel());
        server.verify();
    }
}
