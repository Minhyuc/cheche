package com.cheche.facility.domain;

/** MVP pricing policy used consistently by facility discovery and reservation APIs. */
public final class FacilityPricingPolicy {
    public static final int UNVERIFIED_FEE = 3333;

    private FacilityPricingPolicy() {}

    public static int resolve(Facility facility) {
        return facility.getUsageFee() == null ? UNVERIFIED_FEE : facility.getUsageFee();
    }
}
