package com.cheche.facility.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.NaturalLanguageSearchRequest;
import com.cheche.facility.dto.NaturalLanguageSearchResponse;
import com.cheche.facility.repository.FacilityRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class UserFacilityServiceTest {
    private FacilityRepository repository;
    private SeoulPublicFacilityClient seoulPublicFacilityClient;
    private KspoSportsValueClient kspoSportsValueClient;
    private UserFacilityService service;

    @BeforeEach
    void setUp() {
        repository = mock(FacilityRepository.class);
        seoulPublicFacilityClient = mock(SeoulPublicFacilityClient.class);
        kspoSportsValueClient = mock(KspoSportsValueClient.class);
        when(seoulPublicFacilityClient.findByRegion(anyString())).thenReturn(List.of());
        when(kspoSportsValueClient.findAll()).thenReturn(List.of());
        service = new UserFacilityService(repository, seoulPublicFacilityClient, kspoSportsValueClient);
    }

    @Test
    void naturalLanguageSearchRanksNameAndTypeMatches() {
        Facility pool = facility(1L, "강남스포츠문화센터", "수영장", "서울특별시 강남구");
        Facility court = facility(2L, "강남구민체육관", "배드민턴장", "서울특별시 강남구");
        when(repository.findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus.OPERATING, "11680"))
                .thenReturn(List.of(court, pool));

        NaturalLanguageSearchResponse response = service.search(
                new NaturalLanguageSearchRequest("강남에서 수영할 수 있는 곳"), "11680");

        assertFalse(response.empty());
        assertEquals(2, response.totalCount());
        assertEquals("강남스포츠문화센터", response.facilities().get(0).name());
    }

    @Test
    void searchReturnsHelpfulEmptyState() {
        when(repository.findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus.OPERATING, "11680"))
                .thenReturn(List.of());

        NaturalLanguageSearchResponse response = service.search(
                new NaturalLanguageSearchRequest("클라이밍"), "11680");

        assertTrue(response.empty());
        assertEquals(3, response.suggestions().size());
    }

    @Test
    void usageGuideDoesNotExposeManagerInformation() {
        Facility pool = facility(1L, "강남수영장", "수영장", "서울특별시 강남구");
        when(repository.findById(1L)).thenReturn(Optional.of(pool));

        var guide = service.usageGuide(1L, "11680");

        assertTrue(guide.reservable());
        assertEquals("강남수영장", guide.facilityName());
    }

    @Test
    void searchIncludesSeoulOpenApiFacilitiesInUsersRegion() {
        when(repository.findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus.OPERATING, "11680"))
                .thenReturn(List.of());
        when(seoulPublicFacilityClient.findByRegion("11680")).thenReturn(List.of(
                new com.cheche.facility.dto.UserFacilityCard(null, "SVC-1", "SEOUL_OPEN_API",
                        "대치유수지체육공원", "풋살장", "서울특별시 강남구", "대치유수지체육공원",
                        "02-0000-0000", null, "06:00", "22:00", FacilityStatus.OPERATING, "접수중")));

        NaturalLanguageSearchResponse response = service.search(
                new NaturalLanguageSearchRequest("강남 풋살장"), "11680");

        assertEquals(1, response.totalCount());
        assertEquals("SEOUL_OPEN_API", response.facilities().get(0).source());
        assertEquals("대치유수지체육공원", response.facilities().get(0).name());
    }

    @Test
    void searchIncludesKspoOfficialOperatingFacilities() {
        when(repository.findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus.OPERATING, "11680"))
                .thenReturn(List.of());
        when(kspoSportsValueClient.findAll()).thenReturn(List.of(
                new com.cheche.facility.dto.UserFacilityCard(null, "KSPO-1", "KSPO_OPEN_API",
                        "다목적체육관", "실내체육관", "KSPO 스포츠가치센터", "본관 1층",
                        null, null, "09:00", "20:00", FacilityStatus.OPERATING,
                        "KSPO 공식 · 회차 최대 20명")));

        NaturalLanguageSearchResponse response = service.search(
                new NaturalLanguageSearchRequest("KSPO 다목적체육관"), "11680");

        assertEquals(1, response.totalCount());
        assertEquals("KSPO_OPEN_API", response.facilities().get(0).source());
    }

    private Facility facility(Long id, String name, String type, String regionName) {
        Facility facility = new Facility(name, type, "11680", regionName,
                regionName + " 체육관로 1", "02-0000-0000", 1001L, "정상 운영 중");
        ReflectionTestUtils.setField(facility, "id", id);
        return facility;
    }
}
