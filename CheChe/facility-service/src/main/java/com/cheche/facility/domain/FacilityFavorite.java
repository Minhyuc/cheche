package com.cheche.facility.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "facility_favorites", uniqueConstraints = {
        @UniqueConstraint(name = "uk_facility_favorites_user_facility", columnNames = {"userId", "facilityId"})
}, indexes = {
        @Index(name = "idx_facility_favorites_user", columnList = "userId")
})
public class FacilityFavorite {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long facilityId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected FacilityFavorite() {}

    public FacilityFavorite(Long userId, Long facilityId) {
        this.userId = userId;
        this.facilityId = facilityId;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public Long getUserId() { return userId; }
    public Long getFacilityId() { return facilityId; }
}
