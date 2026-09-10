package com.cheche.admin.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "administrators", indexes = {
        @Index(name = "idx_administrators_region", columnList = "regionCode"),
        @Index(name = "idx_administrators_role_status", columnList = "role,status")
})
public class Administrator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long userId;

    @Column(nullable = false, length = 80)
    private String name;

    @Column(length = 180)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AdminRole role = AdminRole.REGIONAL_ADMIN;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AdminStatus status = AdminStatus.ACTIVE;

    @Column(length = 20)
    private String regionCode;

    @Column(length = 80)
    private String regionName;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Administrator() {}

    public Administrator(Long userId, String name, String email) {
        this.userId = userId;
        this.name = name;
        this.email = email;
    }

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public void syncIdentity(String name, String email) {
        this.name = name;
        this.email = email;
    }

    public void assignRegion(String regionCode, String regionName) {
        this.regionCode = regionCode;
        this.regionName = regionName;
    }

    public void changeRole(AdminRole role) { this.role = role; }
    public void changeStatus(AdminStatus status) { this.status = status; }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public String getName() { return name; }
    public String getEmail() { return email; }
    public AdminRole getRole() { return role; }
    public AdminStatus getStatus() { return status; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
