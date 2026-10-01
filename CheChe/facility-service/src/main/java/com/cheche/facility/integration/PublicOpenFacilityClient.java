package com.cheche.facility.integration;

import java.util.List;

public interface PublicOpenFacilityClient {
    FetchResult fetchAll();

    record FetchResult(int totalCount, List<PublicOpenFacilityItem> items) {}
}
