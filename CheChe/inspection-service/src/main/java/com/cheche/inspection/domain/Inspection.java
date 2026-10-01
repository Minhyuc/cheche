package com.cheche.inspection.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inspections", indexes = {
        @Index(name = "idx_inspections_region_action", columnList = "regionCode,actionStatus"),
        @Index(name = "idx_inspections_facility_created", columnList = "facilityId,createdAt"),
        @Index(name = "idx_inspections_defect", columnList = "defectType,severity")
})
public class Inspection {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long facilityId;
    @Column(nullable = false, length = 120)
    private String facilityName;
    @Column(nullable = false, length = 20)
    private String regionCode;
    @Column(nullable = false, length = 80)
    private String regionName;
    @Column(nullable = false)
    private Long reporterUserId;
    @Column(nullable = false, length = 500)
    private String photoUrl;
    @Column(nullable = false, length = 240)
    private String locationDescription;
    @Column(length = 1200)
    private String note;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DefectType defectType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Severity severity;
    @Column(nullable = false)
    private double confidence;

    @Lob
    @Column(nullable = false)
    private String checklist;
    @Lob
    @Column(nullable = false)
    private String similarCases;
    @Lob
    @Column(nullable = false)
    private String reportSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActionStatus actionStatus = ActionStatus.REPORTED;
    @Column(length = 1200)
    private String actionNote;
    private Boolean confirmed = false;
    private LocalDateTime confirmedAt;
    private Long confirmedByUserId;
    @Column(length = 1200)
    private String confirmedDetail;
    private Boolean actionRequired;
    private LocalDate actionDueDate;
    private LocalDateTime resolvedAt;
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Inspection() {}

    public Inspection(Long facilityId, String facilityName, String regionCode, String regionName,
                      Long reporterUserId, String photoUrl, String locationDescription, String note,
                      DefectAnalysis analysis) {
        this.facilityId = facilityId;
        this.facilityName = facilityName;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.reporterUserId = reporterUserId;
        this.photoUrl = photoUrl;
        this.locationDescription = locationDescription;
        this.note = note;
        this.defectType = analysis.defectType();
        this.severity = analysis.severity();
        this.confidence = analysis.confidence();
        this.checklist = String.join("\n", analysis.checklist());
        this.similarCases = String.join("\n", analysis.similarCases());
        this.reportSummary = analysis.reportSummary();
    }

    @PrePersist void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void updateAction(ActionStatus status, String actionNote) {
        this.actionStatus = status;
        this.actionNote = actionNote;
        this.resolvedAt = status == ActionStatus.RESOLVED ? LocalDateTime.now() : null;
    }

    public void confirm(DefectType defectType, Severity severity, String locationDescription,
                        String confirmedDetail, boolean actionRequired, LocalDate actionDueDate,
                        Long confirmedByUserId) {
        this.defectType = defectType;
        this.severity = severity;
        this.locationDescription = locationDescription;
        this.confirmedDetail = confirmedDetail;
        this.actionRequired = actionRequired;
        this.actionDueDate = actionRequired ? actionDueDate : null;
        this.confirmed = true;
        this.confirmedAt = LocalDateTime.now();
        this.confirmedByUserId = confirmedByUserId;
        this.actionStatus = actionRequired ? ActionStatus.ACTION_SCHEDULED : ActionStatus.RESOLVED;
        this.resolvedAt = actionRequired ? null : LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getFacilityId() { return facilityId; }
    public String getFacilityName() { return facilityName; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public Long getReporterUserId() { return reporterUserId; }
    public String getPhotoUrl() { return photoUrl; }
    public String getLocationDescription() { return locationDescription; }
    public String getNote() { return note; }
    public DefectType getDefectType() { return defectType; }
    public Severity getSeverity() { return severity; }
    public double getConfidence() { return confidence; }
    public String getChecklist() { return checklist; }
    public String getSimilarCases() { return similarCases; }
    public String getReportSummary() { return reportSummary; }
    public ActionStatus getActionStatus() { return actionStatus; }
    public String getActionNote() { return actionNote; }
    public boolean isConfirmed() { return Boolean.TRUE.equals(confirmed); }
    public LocalDateTime getConfirmedAt() { return confirmedAt; }
    public Long getConfirmedByUserId() { return confirmedByUserId; }
    public String getConfirmedDetail() { return confirmedDetail; }
    public Boolean getActionRequired() { return actionRequired; }
    public LocalDate getActionDueDate() { return actionDueDate; }
    public LocalDateTime getResolvedAt() { return resolvedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
