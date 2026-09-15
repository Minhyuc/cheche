package com.cheche.login.controller;

import com.cheche.login.dto.RegionUpdateRequest;
import com.cheche.login.dto.UserProfileResponse;
import com.cheche.login.service.UserProfileService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserProfileController {
    private final UserProfileService service;

    public UserProfileController(UserProfileService service) {
        this.service = service;
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.get(userId));
    }

    @PutMapping("/me/region")
    public ResponseEntity<UserProfileResponse> updateRegion(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RegionUpdateRequest request) {
        return ResponseEntity.ok(service.updateRegion(userId, request));
    }

    @GetMapping("/regions")
    public ResponseEntity<List<Map<String, String>>> regions() {
        return ResponseEntity.ok(service.districts().entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> Map.of("regionCode", entry.getKey(), "regionName", entry.getValue()))
                .toList());
    }
}
