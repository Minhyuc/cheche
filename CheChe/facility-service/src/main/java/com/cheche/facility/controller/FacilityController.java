package com.cheche.facility.controller;

import com.cheche.facility.domain.AdminRole;
import com.cheche.facility.dto.FacilityRequest;
import com.cheche.facility.dto.FacilityResponse;
import com.cheche.facility.service.FacilityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/facilities")
public class FacilityController {
    private final FacilityService service;

    public FacilityController(FacilityService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<FacilityResponse>> list(
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok(service.list(role, regionCode));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityResponse> get(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok(service.get(id, role, regionCode));
    }

    @PostMapping
    public ResponseEntity<FacilityResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode,
            @Valid @RequestBody FacilityRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request, userId, role, regionCode));
    }

    @PutMapping("/{id}")
    public ResponseEntity<FacilityResponse> update(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode,
            @Valid @RequestBody FacilityRequest request) {
        return ResponseEntity.ok(service.update(id, request, role, regionCode));
    }
}
