package com.cheche.inspection.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cheche.inspection.domain.*;
import com.cheche.inspection.dto.FacilitySummary;
import com.cheche.inspection.repository.FacilityReportRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class UserFacilityReportServiceTest {
    private FacilityReportRepository repository;
    private PhotoStorage photoStorage;
    private FacilityServiceClient facilityClient;
    private UserFacilityReportService service;

    @BeforeEach
    void setUp() {
        repository = mock(FacilityReportRepository.class);
        photoStorage = mock(PhotoStorage.class);
        facilityClient = mock(FacilityServiceClient.class);
        service = new UserFacilityReportService(repository, photoStorage, facilityClient);
    }

    @Test
    void userCanSubmitPhotoAndCommentForFacilityInOwnRegion() {
        var photo = new MockMultipartFile("photo", "damage.jpg", "image/jpeg", new byte[]{1});
        when(facilityClient.get(10L, 20L, "11680")).thenReturn(new FacilitySummary(10L, "강남체육관",
                "체육관", "서울특별시 강남구", "서울 강남구", "02-0000-0000",
                "OPERATING", "운영 중", null, true, "/guide"));
        when(photoStorage.store(photo)).thenReturn("/inspection-photos/photo.jpg");
        when(repository.save(any(FacilityReport.class))).thenAnswer(invocation -> {
            FacilityReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 1L);
            ReflectionTestUtils.setField(report, "createdAt", java.time.LocalDateTime.now());
            ReflectionTestUtils.setField(report, "updatedAt", java.time.LocalDateTime.now());
            return report;
        });

        var response = service.create(20L, "11680", 10L, ReportCategory.REPAIR,
                "풋살장 1번 골대", "그물이 찢어져 수리가 필요합니다.", photo);

        assertEquals(ReportStatus.RECEIVED, response.status());
        assertEquals("수리 필요", response.categoryLabel());
        assertEquals("/inspection-photos/photo.jpg", response.photoUrl());
        verify(facilityClient).get(10L, 20L, "11680");
    }

    @Test
    void userCannotReadAnotherUsersReport() {
        FacilityReport report = new FacilityReport(99L, 10L, "강남체육관", "11680",
                "서울특별시 강남구", ReportCategory.IMPROVEMENT, "탈의실", "조명 개선 필요",
                "/inspection-photos/photo.jpg");
        when(repository.findById(1L)).thenReturn(Optional.of(report));

        assertThrows(ResponseStatusException.class, () -> service.getMine(20L, 1L));
    }
}
