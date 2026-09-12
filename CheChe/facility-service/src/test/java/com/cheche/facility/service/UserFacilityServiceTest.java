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
    private UserFacilityService service;

    @BeforeEach
    void setUp() {
        repository = mock(FacilityRepository.class);
        service = new UserFacilityService(repository);
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

    private Facility facility(Long id, String name, String type, String regionName) {
        Facility facility = new Facility(name, type, "11680", regionName,
                regionName + " 체육관로 1", "02-0000-0000", 1001L, "정상 운영 중");
        ReflectionTestUtils.setField(facility, "id", id);
        return facility;
    }
}
