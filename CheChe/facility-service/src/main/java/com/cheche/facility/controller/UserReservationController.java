package com.cheche.facility.controller;

import com.cheche.facility.dto.*;
import com.cheche.facility.service.ReservationService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user/reservations")
public class UserReservationController {
    private final ReservationService service;

    public UserReservationController(ReservationService service) { this.service = service; }

    @PostMapping
    public ResponseEntity<ReservationResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader("X-User-Region") String regionCode,
            @Valid @RequestBody ReservationCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(userId, regionCode, request));
    }

    @GetMapping
    public ResponseEntity<List<ReservationResponse>> list(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.list(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ReservationResponse> get(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(service.get(userId, id));
    }

    @PatchMapping("/{id}/cancel")
    public ResponseEntity<ReservationResponse> cancel(
            @RequestHeader("X-User-Id") Long userId, @PathVariable Long id) {
        return ResponseEntity.ok(service.cancel(userId, id));
    }

    @GetMapping("/availability")
    public ResponseEntity<AvailabilityResponse> availability(
            @RequestHeader("X-User-Region") String regionCode,
            @RequestParam Long facilityId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(service.availability(regionCode, facilityId, date));
    }
}
