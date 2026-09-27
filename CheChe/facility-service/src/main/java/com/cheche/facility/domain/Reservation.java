package com.cheche.facility.domain;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservations", indexes = {
        @Index(name = "idx_reservations_user_date", columnList = "userId,reservationDate"),
        @Index(name = "idx_reservations_facility_slot", columnList = "facilityId,reservationDate,startTime,status")
})
public class Reservation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long facilityId;

    @Column(nullable = false, length = 120)
    private String facilityName;

    @Column(nullable = false, length = 20)
    private String regionCode;

    @Column(nullable = false, length = 80)
    private String regionName;

    @Column(nullable = false)
    private LocalDate reservationDate;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(nullable = false)
    private int participantCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status = ReservationStatus.CONFIRMED;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    private LocalDateTime cancelledAt;

    protected Reservation() {}

    public Reservation(Long userId, Facility facility, LocalDate reservationDate,
                       LocalTime startTime, LocalTime endTime, int participantCount) {
        this.userId = userId;
        this.facilityId = facility.getId();
        this.facilityName = facility.getName();
        this.regionCode = facility.getRegionCode();
        this.regionName = facility.getRegionName();
        this.reservationDate = reservationDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.participantCount = participantCount;
    }

    @PrePersist
    void onCreate() { createdAt = LocalDateTime.now(); }

    public void cancel() {
        status = ReservationStatus.CANCELLED;
        cancelledAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getFacilityId() { return facilityId; }
    public String getFacilityName() { return facilityName; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public LocalDate getReservationDate() { return reservationDate; }
    public LocalTime getStartTime() { return startTime; }
    public LocalTime getEndTime() { return endTime; }
    public int getParticipantCount() { return participantCount; }
    public ReservationStatus getStatus() { return status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCancelledAt() { return cancelledAt; }
}
