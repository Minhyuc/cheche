package com.cheche.facility.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cheche.facility.domain.AdminRole;
import com.cheche.facility.domain.Facility;
import com.cheche.facility.dto.PublicFacilitySyncResponse;
import com.cheche.facility.integration.KspoFacilityClient;
import com.cheche.facility.integration.KspoFacilityItem;
import com.cheche.facility.repository.FacilityRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class PublicFacilitySyncServiceTest {
    private final FacilityRepository repository = mock(FacilityRepository.class);
    private final KspoFacilityClient client = mock(KspoFacilityClient.class);
    private final PublicFacilitySyncService service = new PublicFacilitySyncService(repository, client);

    @Test
    void regionalAdminSyncsOnlyAssignedSeoulDistrict() {
        when(client.fetchPublicFacilities("서울특별시", "강남구"))
                .thenReturn(new KspoFacilityClient.FetchResult(2, List.of(
                item("강남수영장", "서울특별시 강남구 테헤란로 1", "https://example/1"),
                item("송파체육관", "서울특별시 송파구 올림픽로 1", "https://example/2"))));
        when(repository.findAllBySourceAndRegionCode(PublicFacilitySyncService.SOURCE, "11680"))
                .thenReturn(List.of());
        when(repository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PublicFacilitySyncResponse response = service.sync(10L, AdminRole.REGIONAL_ADMIN, "11680");

        assertEquals(2, response.scannedCount());
        assertEquals(1, response.matchedCount());
        assertEquals(1, response.createdCount());
        verify(repository).save(any(Facility.class));
    }

    @Test
    void superUserSyncsAllSeoulDistricts() {
        when(client.fetchPublicFacilities("서울특별시", null))
                .thenReturn(new KspoFacilityClient.FetchResult(2, List.of(
                        item("강남수영장", "서울특별시 강남구 테헤란로 1", "FACI-1"),
                        item("송파체육관", "서울특별시 송파구 올림픽로 1", "FACI-2"))));
        when(repository.findAllBySource(PublicFacilitySyncService.SOURCE)).thenReturn(List.of());
        when(repository.save(any(Facility.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PublicFacilitySyncResponse response = service.sync(1L, AdminRole.SUPER_USER, null);

        assertEquals("11", response.regionCode());
        assertEquals(2, response.createdCount());
    }

    @Test
    void rejectsRegionalAdminOutsideSeoulBeforeCallingProvider() {
        assertThrows(ResponseStatusException.class,
                () -> service.sync(10L, AdminRole.REGIONAL_ADMIN, "26110"));
        verify(client, never()).fetchPublicFacilities(any(), any());
    }

    private KspoFacilityItem item(String title, String address, String url) {
        String district = address.contains("강남구") ? "강남구" : "송파구";
        return new KspoFacilityItem(url, title, "공공", "체육시설업", "체육관", "정상운영",
                address, null, null, null, "02-0000-0000", null, url, "서울특별시", district);
    }
}
