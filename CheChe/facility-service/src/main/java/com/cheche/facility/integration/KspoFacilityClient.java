package com.cheche.facility.integration;

import java.util.List;

public interface KspoFacilityClient {
    FetchResult fetchPublicFacilities(String province, String district);

    record FetchResult(int totalCount, List<KspoFacilityItem> items) {}
}
