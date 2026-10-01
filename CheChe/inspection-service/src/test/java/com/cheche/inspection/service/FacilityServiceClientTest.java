package com.cheche.inspection.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class FacilityServiceClientTest {
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) server.stop(0);
    }

    @Test
    void forwardsUserAndRegionHeadersToFacilityService() throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/api/user/facilities/1676", exchange -> {
            if (!"20".equals(exchange.getRequestHeaders().getFirst("X-User-Id"))
                    || !"11545".equals(exchange.getRequestHeaders().getFirst("X-User-Region"))) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            byte[] body = ("{\"id\":1676,\"name\":\"검증 전용 시설\",\"type\":\"체육시설\","
                    + "\"regionName\":\"서울특별시 금천구\",\"address\":\"서울 금천구\","
                    + "\"status\":\"CLOSED\",\"statusLabel\":\"운영 종료\","
                    + "\"reservable\":false,\"usageGuidePath\":\"/guide\"}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        FacilityServiceClient client = new FacilityServiceClient(RestClient.builder(),
                "http://127.0.0.1:" + server.getAddress().getPort());

        var response = client.get(1676L, 20L, "11545");

        assertEquals(1676L, response.id());
        assertEquals("CLOSED", response.status());
    }
}
