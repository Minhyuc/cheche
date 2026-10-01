package com.cheche.inspection.controller;

import com.cheche.inspection.domain.ReportCategory;
import com.cheche.inspection.dto.FacilityReportResponse;
import com.cheche.inspection.service.UserFacilityReportService;
import java.util.List;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/user/reports")
public class UserFacilityReportController {
    private final UserFacilityReportService service;

    public UserFacilityReportController(UserFacilityReportService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<FacilityReportResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode,
            @RequestParam Long facilityId,
            @RequestParam ReportCategory category,
            @RequestParam String locationDescription,
            @RequestParam String comment,
            @RequestPart("photo") MultipartFile photo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId, regionCode,
                facilityId, category, locationDescription, comment, photo));
    }

    @GetMapping
    public ResponseEntity<List<FacilityReportResponse>> mine(
            @RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.mine(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FacilityReportResponse> getMine(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ResponseEntity.ok(service.getMine(userId, id));
    }
}
