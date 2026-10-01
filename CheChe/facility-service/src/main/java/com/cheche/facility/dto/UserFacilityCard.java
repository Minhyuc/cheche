package com.cheche.facility.dto;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityPricingPolicy;
import com.cheche.facility.domain.FacilityStatus;
import java.util.List;

public record UserFacilityCard(
        Long id,
        String externalId,
        String source,
        String name,
        String type,
        String regionName,
        String address,
        String phone,
        String imageUrl,
        String openingTime,
        String closingTime,
        FacilityStatus status,
        String statusLabel,
        Double distanceKm,
        Integer usageFee,
        String nextAvailableTime,
        boolean favorite,
        List<String> tags
) {
    public static final String DEFAULT_IMAGE_URL = "/images/facility-default.svg";
    public UserFacilityCard(Long id, String externalId, String source, String name, String type,
                            String regionName, String address, String phone, String imageUrl,
                            String openingTime, String closingTime, FacilityStatus status,
                            String statusLabel) {
        this(id, externalId, source, name, type, regionName, address, phone, imageUrl,
                openingTime, closingTime, status, statusLabel, null, 5000, null, false,
                type == null || type.isBlank() ? List.of() : List.of(type));
    }

    public static UserFacilityCard from(Facility facility) {
        String openingTime = facility.getWeekdayOpeningTime() == null ? "08:00" : facility.getWeekdayOpeningTime();
        String closingTime = facility.getWeekdayClosingTime() == null ? "22:00" : facility.getWeekdayClosingTime();
        Integer fee = FacilityPricingPolicy.resolve(facility);
        return new UserFacilityCard(facility.getId(), facility.getExternalId(),
                facility.getSource() == null ? "CHECHE" : facility.getSource(), facility.getName(),
                facility.getType(), facility.getRegionName(), facility.getAddress(), facility.getPhone(),
                imageOrDefault(facility.getImageUrl()), openingTime, closingTime, facility.getStatus(), statusLabel(facility.getStatus()),
                null, fee, null, false, tags(facility));
    }

    private static List<String> tags(Facility facility) {
        java.util.ArrayList<String> tags = new java.util.ArrayList<>();
        if (facility.getType() != null && !facility.getType().isBlank()) tags.add(facility.getType());
        if (facility.getApplicationMethod() != null && !facility.getApplicationMethod().isBlank()) {
            tags.add(facility.getApplicationMethod());
        }
        return List.copyOf(tags);
    }

    public UserFacilityCard withFavorite(boolean value) {
        return new UserFacilityCard(id, externalId, source, name, type, regionName, address, phone,
                imageUrl, openingTime, closingTime, status, statusLabel, distanceKm, usageFee,
                nextAvailableTime, value, tags);
    }

    public UserFacilityCard withDistance(Double value) {
        return new UserFacilityCard(id, externalId, source, name, type, regionName, address, phone,
                imageUrl, openingTime, closingTime, status, statusLabel, value, usageFee,
                nextAvailableTime, favorite, tags);
    }

    public UserFacilityCard withDefaultImage() {
        if (imageUrl != null && !imageUrl.isBlank()) return this;
        return new UserFacilityCard(id, externalId, source, name, type, regionName, address, phone,
                DEFAULT_IMAGE_URL, openingTime, closingTime, status, statusLabel, distanceKm, usageFee,
                nextAvailableTime, favorite, tags);
    }

    private static String imageOrDefault(String value) {
        return value == null || value.isBlank() ? DEFAULT_IMAGE_URL : value;
    }

    private static String statusLabel(FacilityStatus status) {
        return switch (status) {
            case OPERATING -> "운영 중";
            case UNDER_INSPECTION -> "점검 중";
            case CLOSED -> "운영 종료";
        };
    }
}
