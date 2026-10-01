package com.cheche.facility.dto;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityPricingPolicy;
import com.cheche.facility.domain.FacilityStatus;
import java.util.List;

public record UserFacilityDetailResponse(
        Long id,
        String externalId,
        String source,
        String name,
        String type,
        String regionName,
        String address,
        String phone,
        String imageUrl,
        Double distanceKm,
        FacilityStatus status,
        String statusLabel,
        String publicNotice,
        String openingTime,
        String closingTime,
        int usageFee,
        List<String> availableFacilities,
        List<String> amenities,
        boolean favorite,
        boolean reservable,
        String sourceUrl,
        String usageGuidePath,
        String reservationOptionsPath,
        String weekendOpeningTime,
        String weekendClosingTime,
        String feeInfo,
        Integer capacity,
        String applicationMethod,
        String closedDays,
        Double latitude,
        Double longitude
) {
    public static UserFacilityDetailResponse from(Facility facility, boolean favorite) {
        return from(facility, favorite, null);
    }

    public static UserFacilityDetailResponse from(Facility facility, boolean favorite, Double distanceKm) {
        return new UserFacilityDetailResponse(
                facility.getId(), facility.getExternalId(),
                facility.getSource() == null ? "CHECHE" : facility.getSource(),
                facility.getName(), facility.getType(), facility.getRegionName(),
                facility.getAddress(), facility.getPhone(),
                facility.getImageUrl() == null || facility.getImageUrl().isBlank()
                        ? UserFacilityCard.DEFAULT_IMAGE_URL : facility.getImageUrl(),
                distanceKm, facility.getStatus(),
                statusLabel(facility.getStatus()), facility.getPublicNotice(),
                defaultValue(facility.getWeekdayOpeningTime(), "08:00"),
                defaultValue(facility.getWeekdayClosingTime(), "22:00"),
                FacilityPricingPolicy.resolve(facility),
                facility.getType() == null ? List.of() : List.of(facility.getType()),
                split(facility.getAmenities()), favorite,
                facility.getStatus() == FacilityStatus.OPERATING, facility.getSourceUrl(),
                "/api/user/facilities/" + facility.getId() + "/usage-guide",
                "/api/user/reservations/options?facilityId=" + facility.getId(),
                facility.getWeekendOpeningTime(), facility.getWeekendClosingTime(), facility.getFeeInfo(),
                facility.getCapacity(), facility.getApplicationMethod(), facility.getClosedDays(),
                facility.getLatitude(), facility.getLongitude());
    }

    private static String defaultValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static List<String> split(String value) {
        if (value == null || value.isBlank()) return List.of();
        return java.util.Arrays.stream(value.split("[,/+]"))
                .map(String::trim).filter(item -> !item.isBlank()).distinct().toList();
    }

    private static String statusLabel(FacilityStatus status) {
        return switch (status) {
            case OPERATING -> "운영 중";
            case UNDER_INSPECTION -> "점검 중";
            case CLOSED -> "운영 종료";
        };
    }
}
