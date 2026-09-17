package com.cheche.admin.controller;

import com.cheche.admin.domain.AdminRole;
import com.cheche.admin.dto.*;
import com.cheche.admin.service.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admins")
public class AdminController {
    private final AdminService service;

    public AdminController(AdminService service) {
        this.service = service;
    }

    /** Called by the future login-service after a successful login. */
    @PostMapping("/sync")
    public ResponseEntity<AdminResponse> sync(@Valid @RequestBody AdminSyncRequest request) {
        return ResponseEntity.ok(service.sync(request));
    }

    @GetMapping("/me")
    public ResponseEntity<AdminResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return ResponseEntity.ok(service.getMe(userId));
    }

    @GetMapping("/regions")
    public ResponseEntity<List<RegionOptionResponse>> regions() {
        return ResponseEntity.ok(service.regions());
    }

    @PutMapping("/me/region")
    public ResponseEntity<AdminResponse> setupRegion(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RegionSetupRequest request) {
        return ResponseEntity.ok(service.setupMyRegion(userId, request));
    }

    @GetMapping
    public ResponseEntity<List<AdminResponse>> list(
            @RequestHeader("X-User-Role") AdminRole requesterRole) {
        return ResponseEntity.ok(service.listAll(requesterRole));
    }

    @PatchMapping("/{id}/authority")
    public ResponseEntity<AdminResponse> updateAuthority(
            @PathVariable Long id,
            @RequestHeader("X-User-Role") AdminRole requesterRole,
            @Valid @RequestBody AdminAuthorityRequest request) {
        return ResponseEntity.ok(service.updateAuthority(id, request, requesterRole));
    }
}
