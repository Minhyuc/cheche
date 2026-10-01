package com.cheche.facility.domain;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "facilities", indexes = {
        @Index(name = "idx_facilities_region_status", columnList = "regionCode,status"),
        @Index(name = "idx_facilities_region_type", columnList = "regionCode,type")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_facilities_source_external_id", columnNames = {"source", "externalId"})
})
public class Facility {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 40)
    private String type;

    @Column(nullable = false, length = 20)
    private String regionCode;

    @Column(nullable = false, length = 80)
    private String regionName;

    @Column(nullable = false, length = 240)
    private String address;

    @Column(length = 30)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private FacilityStatus status = FacilityStatus.OPERATING;

    @Column(nullable = false)
    private Long managerUserId;

    @Column(length = 1000)
    private String publicNotice;

    @Column(length = 40)
    private String source;

    @Column(length = 160)
    private String externalId;

    @Column(length = 500)
    private String sourceUrl;

    @Column(length = 1000)
    private String imageUrl;

    @Column(length = 5)
    private String weekdayOpeningTime;

    @Column(length = 5)
    private String weekdayClosingTime;

    @Column(length = 5)
    private String weekendOpeningTime;

    @Column(length = 5)
    private String weekendClosingTime;

    private Integer usageFee;

    @Column(length = 500)
    private String feeInfo;

    private Integer capacity;

    @Column(length = 1000)
    private String amenities;

    @Column(length = 240)
    private String applicationMethod;

    @Column(length = 240)
    private String closedDays;

    private Double latitude;

    private Double longitude;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    protected Facility() {}

    public Facility(String name, String type, String regionCode, String regionName,
                    String address, String phone, Long managerUserId, String publicNotice) {
        this.name = name;
        this.type = type;
        this.regionCode = regionCode;
        this.regionName = regionName;
        this.address = address;
        this.phone = phone;
        this.managerUserId = managerUserId;
        this.publicNotice = publicNotice;
    }

    @PrePersist
    void onCreate() { createdAt = updatedAt = LocalDateTime.now(); }
    @PreUpdate
    void onUpdate() { updatedAt = LocalDateTime.now(); }

    public void update(String name, String type, String address, String phone,
                       FacilityStatus status, String publicNotice) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.phone = phone;
        this.status = status;
        this.publicNotice = publicNotice;
    }

    public static Facility fromPublicData(String name, String type, String regionCode, String regionName,
                                          String address, String phone, Long managerUserId,
                                          FacilityStatus status, String source, String externalId, String sourceUrl) {
        Facility facility = new Facility(name, type, regionCode, regionName, address, phone,
                managerUserId, "국민체육진흥공단 전국체육시설 정보 연계");
        facility.source = source;
        facility.externalId = externalId;
        facility.sourceUrl = sourceUrl;
        facility.status = status;
        return facility;
    }

    public void updatePublicData(String name, String type, String address, String phone, String sourceUrl) {
        this.name = name;
        this.type = type;
        this.address = address;
        this.phone = phone;
        this.sourceUrl = sourceUrl;
    }

    public void enrichPublicData(String imageUrl, String weekdayOpeningTime, String weekdayClosingTime,
                                 String weekendOpeningTime, String weekendClosingTime, Integer usageFee,
                                 String feeInfo, Integer capacity, String amenities, String applicationMethod,
                                 String closedDays, Double latitude, Double longitude, String sourceUrl) {
        this.imageUrl = blankToNull(imageUrl);
        this.weekdayOpeningTime = normalizeTime(weekdayOpeningTime);
        this.weekdayClosingTime = normalizeTime(weekdayClosingTime);
        this.weekendOpeningTime = normalizeTime(weekendOpeningTime);
        this.weekendClosingTime = normalizeTime(weekendClosingTime);
        this.usageFee = usageFee;
        this.feeInfo = blankToNull(feeInfo);
        this.capacity = capacity;
        this.amenities = blankToNull(amenities);
        this.applicationMethod = blankToNull(applicationMethod);
        this.closedDays = blankToNull(closedDays);
        this.latitude = latitude;
        this.longitude = longitude;
        if (sourceUrl != null && !sourceUrl.isBlank()) this.sourceUrl = sourceUrl.trim();
    }

    private String normalizeTime(String value) {
        String time = blankToNull(value);
        return time != null && time.matches("\\d{2}:\\d{2}") ? time : null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getType() { return type; }
    public String getRegionCode() { return regionCode; }
    public String getRegionName() { return regionName; }
    public String getAddress() { return address; }
    public String getPhone() { return phone; }
    public FacilityStatus getStatus() { return status; }
    public Long getManagerUserId() { return managerUserId; }
    public String getPublicNotice() { return publicNotice; }
    public String getSource() { return source; }
    public String getExternalId() { return externalId; }
    public String getSourceUrl() { return sourceUrl; }
    public String getImageUrl() { return imageUrl; }
    public String getWeekdayOpeningTime() { return weekdayOpeningTime; }
    public String getWeekdayClosingTime() { return weekdayClosingTime; }
    public String getWeekendOpeningTime() { return weekendOpeningTime; }
    public String getWeekendClosingTime() { return weekendClosingTime; }
    public Integer getUsageFee() { return usageFee; }
    public String getFeeInfo() { return feeInfo; }
    public Integer getCapacity() { return capacity; }
    public String getAmenities() { return amenities; }
    public String getApplicationMethod() { return applicationMethod; }
    public String getClosedDays() { return closedDays; }
    public Double getLatitude() { return latitude; }
    public Double getLongitude() { return longitude; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
