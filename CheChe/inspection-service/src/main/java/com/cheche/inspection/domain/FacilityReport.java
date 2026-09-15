package com.cheche.inspection.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "facility_reports", indexes = {
        @Index(name = "idx_reports_reporter_created", columnList = "reporterUserId,createdAt"),
        @Index(name = "idx_reports_region_status", columnList = "regionCode,status")
})
public class FacilityReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long reporterUserId;
    @Column(nullable = false)
    private Long facilityId;
    @Column(nullable = false, length = 120)
    private String facilityName;
    @Column(nullable = false, length = 20)
    private String regionCode;
    @Column(nullable = false, length = 80)
    private String regionName;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportCategory category;
    @Column(nullable = false, length = 240)
    private String locationDescription;
    @Column(nullable = false, length = 2000)
    private String comment;
    @Column(nullable = false, length = 500)
    private String photoUrl;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ReportStatus status = ReportStatus.RECEIVED;
    @Column(length = 2000)
    private String resolutionNote;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected FacilityReport() {}

    public FacilityReport(Long reporterUserId, Long facilityId, String facilityName,
                          String regionCode, String regionName, ReportCategory category,
                          String locationDescription, String comment, String photoUrl) {
        this.reporterUserId = reporterUserId;
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.category = category;
        this.locationDescription = locationDescription;
        this.comment = comment;
        this.photoUrl = photoUrl;
    }

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getReporterUserId() { return reporterUserId; }
    public Long getFacilityId() { return facilityId; }
    public String getFacilityName() { return facilityName; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public ReportCategory getCategory() { return category; }
    public String getLocationDescription() { return locationDescription; }
    public String getComment() { return comment; }
    public String getPhotoUrl() { return photoUrl; }
    public ReportStatus getStatus() { return status; }
    public String getResolutionNote() { return resolutionNote; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
