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
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ResponseEntity.ok(service.home(userId, regionCode, latitude, longitude));
    }

    @PostMapping("/search")
    public ResponseEntity<NaturalLanguageSearchResponse> search(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @Valid @RequestBody NaturalLanguageSearchRequest request) {
        return ResponseEntity.ok(service.search(userId, request, regionCode, latitude, longitude));
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserFacilityDetailResponse> detail(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ResponseEntity.ok(service.detail(userId, id, regionCode, latitude, longitude));
    }

    @GetMapping("/favorites")
    public ResponseEntity<java.util.List<UserFacilityCard>> favorites(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude) {
        return ResponseEntity.ok(service.favorites(userId, regionCode, latitude, longitude));
    }

    @PostMapping("/{id}/favorite")
    public ResponseEntity<FavoriteResponse> addFavorite(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode) {
        return ResponseEntity.ok(service.addFavorite(userId, id, regionCode));
    }

    @DeleteMapping("/{id}/favorite")
    public ResponseEntity<FavoriteResponse> removeFavorite(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode) {
        return ResponseEntity.ok(service.removeFavorite(userId, id, regionCode));
    }

    @GetMapping("/{id}/usage-guide")
    public ResponseEntity<UsageGuideResponse> usageGuide(
            @PathVariable Long id,
            @RequestHeader("X-User-Region") String regionCode) {
        return ResponseEntity.ok(service.usageGuide(id, regionCode));
    }
}
