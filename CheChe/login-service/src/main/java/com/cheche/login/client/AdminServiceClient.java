package com.cheche.login.client;

import com.cheche.login.dto.AdminProfile;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AdminServiceClient {
    private final RestClient client;

    public AdminServiceClient(RestClient.Builder builder,
                              @Value("${services.admin.base-url:http://localhost:8082}") String baseUrl) {
        this.client = builder.baseUrl(baseUrl).build();
    }

    public AdminProfile sync(Long userId, String username) {
        try {
            return client.post()
                    .uri("/api/admins/sync")
                    .body(Map.of("userId", userId, "username", username))
                    .retrieve()
                    .body(AdminProfile.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관리자 프로필 동기화에 실패했습니다.", exception);
        }
    }

    public AdminProfile get(Long userId) {
        try {
            return client.get()
                    .uri("/api/admins/me")
                    .header("X-User-Id", String.valueOf(userId))
                    .retrieve()
                    .body(AdminProfile.class);
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "관리자 프로필 조회에 실패했습니다.", exception);
        }
    }
}
