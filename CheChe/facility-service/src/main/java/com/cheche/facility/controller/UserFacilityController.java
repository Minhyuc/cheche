package com.cheche.facility.controller;

import com.cheche.facility.dto.*;
import com.cheche.facility.service.UserFacilityService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/facilities")
public class UserFacilityController {
    private final UserFacilityService service;

    public UserFacilityController(UserFacilityService service) {
        this.service = service;
    }

    @GetMapping("/home")
    public ResponseEntity<UserHomeResponse> home(
            @RequestHeader("X-User-Region") String regionCode) {
        return ResponseEntity.ok(service.home(regionCode));
    }

    @PostMapping("/search")
    public ResponseEntity<NaturalLanguageSearchResponse> search(
            @RequestHeader("X-User-Region") String regionCode,
            @Valid @RequestBody NaturalLanguageSearchRequest request) {
        return ResponseEntity.ok(service.search(request, regionCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserFacilityDetailResponse> detail(
            @PathVariable Long id,
            @RequestHeader("X-User-Region") String regionCode) {
        return ResponseEntity.ok(service.detail(id, regionCode));
    }

    @GetMapping("/{id}/usage-guide")
    public ResponseEntity<UsageGuideResponse> usageGuide(
            @PathVariable Long id,
            @RequestHeader("X-User-Region") String regionCode) {
        return ResponseEntity.ok(service.usageGuide(id, regionCode));
    }
}
