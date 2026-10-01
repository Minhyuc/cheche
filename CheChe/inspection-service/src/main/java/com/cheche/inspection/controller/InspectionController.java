package com.cheche.inspection.controller;

import com.cheche.inspection.domain.AdminRole;
import com.cheche.inspection.dto.*;
import com.cheche.inspection.service.InspectionService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/inspections")
public class InspectionController {
    private final InspectionService service;

    public InspectionController(InspectionService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<InspectionResponse>> list(
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok(service.list(role, regionCode));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<InspectionResponse> create(
            @RequestParam Long facilityId,
            @RequestParam String facilityName,
            @RequestParam String facilityRegionCode,
            @RequestParam String facilityRegionName,
            @RequestParam String locationDescription,
            @RequestParam(required = false) String note,
            @RequestParam(required = false) String suspectedDefect,
            @RequestPart MultipartFile photo,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String adminRegionCode) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(facilityId, facilityName,
                facilityRegionCode, facilityRegionName, locationDescription, note, suspectedDefect,
                photo, userId, role, adminRegionCode));
    }

    @GetMapping("/facilities/{facilityId}/history")
    public ResponseEntity<List<InspectionResponse>> facilityHistory(
            @PathVariable Long facilityId,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok(service.facilityHistory(facilityId, role, regionCode));
    }

    @GetMapping("/open")
    public ResponseEntity<List<InspectionResponse>> open(
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok(service.open(role, regionCode));
    }

    @GetMapping("/dashboard")
    public ResponseEntity<DashboardResponse> dashboard(
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok(service.dashboard(role, regionCode));
    }

    @PatchMapping("/{id}/action")
    public ResponseEntity<InspectionResponse> updateAction(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode,
            @Valid @RequestBody ActionUpdateRequest request) {
        return ResponseEntity.ok(service.updateAction(id, request, role, regionCode));
    }

    @PatchMapping("/{id}/confirmation")
    public ResponseEntity<InspectionResponse> confirm(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode,
            @Valid @RequestBody DefectConfirmationRequest request) {
        return ResponseEntity.ok(service.confirm(id, request, userId, role, regionCode));
    }

    @GetMapping("/super/regions/safety")
    public ResponseEntity<List<RegionalSafetySummaryResponse>> regionalSafety(
            @RequestHeader("X-User-Role") AdminRole role) {
        return ResponseEntity.ok(service.regionalSafety(role));
    }

    @GetMapping("/super/recurring-defects")
    public ResponseEntity<List<RecurringDefectResponse>> recurringDefects(
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestParam(defaultValue = "2") int minimumOccurrences) {
        return ResponseEntity.ok(service.recurringDefects(role, minimumOccurrences));
    }

    @GetMapping(value = "/{id}/report", produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> report(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") AdminRole role,
            @RequestHeader(value = "X-User-Region", required = false) String regionCode) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=cheche-inspection-" + id + ".txt")
                .body(service.report(id, role, regionCode));
    }

    @GetMapping("/public/facilities/{facilityId}/status")
    public ResponseEntity<List<PublicInspectionStatusResponse>> publicStatus(@PathVariable Long facilityId) {
        return ResponseEntity.ok(service.publicFacilityStatus(facilityId));
    }
}
