package com.cheche.inspection.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cheche.inspection.domain.*;
import com.cheche.inspection.dto.DefectConfirmationRequest;
import com.cheche.inspection.repository.InspectionRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class InspectionServiceTest {
    private InspectionRepository repository;
    private InspectionService service;

    @BeforeEach
    void setUp() {
        repository = mock(InspectionRepository.class);
        service = new InspectionService(repository, mock(PhotoStorage.class), mock(DefectAnalysisPort.class));
    }

    @Test
    void confirmsCorrectedAnalysisAndSchedulesAction() {
        Inspection inspection = inspection(1L, "서울체육관", "11", "서울특별시", DefectType.OTHER, Severity.LOW);
        when(repository.findById(7L)).thenReturn(Optional.of(inspection));
        LocalDate dueDate = LocalDate.now().plusDays(5);

        var response = service.confirm(7L,
                new DefectConfirmationRequest(DefectType.CRACK, Severity.HIGH, "배드민턴장 A 벽면",
                        "균열 길이 32cm 확인", true, dueDate),
                99L, AdminRole.REGIONAL_ADMIN, "11");

        assertThat(response.confirmed()).isTrue();
        assertThat(response.confirmedByUserId()).isEqualTo(99L);
        assertThat(response.defectType()).isEqualTo(DefectType.CRACK);
        assertThat(response.severity()).isEqualTo(Severity.HIGH);
        assertThat(response.actionRequired()).isTrue();
        assertThat(response.actionDueDate()).isEqualTo(dueDate);
        assertThat(response.actionStatus()).isEqualTo(ActionStatus.ACTION_SCHEDULED);
    }

    @Test
    void confirmationWithoutActionResolvesInspection() {
        Inspection inspection = inspection(1L, "서울체육관", "11", "서울특별시", DefectType.OTHER, Severity.LOW);
        when(repository.findById(8L)).thenReturn(Optional.of(inspection));

        var response = service.confirm(8L,
                new DefectConfirmationRequest(DefectType.SURFACE_DAMAGE, Severity.LOW, "출입구",
                        "경미한 표면 손상", false, null),
                99L, AdminRole.SUPER_USER, null);

        assertThat(response.actionStatus()).isEqualTo(ActionStatus.RESOLVED);
        assertThat(response.resolvedAt()).isNotNull();
        assertThat(response.actionDueDate()).isNull();
    }

    @Test
    void calculatesRegionalSafetyScoreFromOpenSeverity() {
        Inspection high = inspection(1L, "A", "11", "서울특별시", DefectType.CRACK, Severity.HIGH);
        Inspection resolved = inspection(2L, "B", "11", "서울특별시", DefectType.CORROSION, Severity.LOW);
        resolved.updateAction(ActionStatus.RESOLVED, "완료");
        when(repository.findAll()).thenReturn(List.of(high, resolved));

        var result = service.regionalSafety(AdminRole.SUPER_USER);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).facilityCount()).isEqualTo(2);
        assertThat(result.get(0).openInspections()).isEqualTo(1);
        assertThat(result.get(0).safetyScore()).isEqualTo(90);
    }

    @Test
    void aggregatesRecurringDefectsByFacilityAndType() {
        Inspection low = inspection(1L, "A", "11", "서울특별시", DefectType.CRACK, Severity.LOW);
        Inspection critical = inspection(1L, "A", "11", "서울특별시", DefectType.CRACK, Severity.CRITICAL);
        when(repository.findAll()).thenReturn(List.of(low, critical));

        var result = service.recurringDefects(AdminRole.SUPER_USER, 2);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).occurrenceCount()).isEqualTo(2);
        assertThat(result.get(0).openCount()).isEqualTo(2);
        assertThat(result.get(0).highestSeverity()).isEqualTo(Severity.CRITICAL);
    }

    @Test
    void rejectsSuperuserAggregateForRegionalAdmin() {
        assertThatThrownBy(() -> service.regionalSafety(AdminRole.REGIONAL_ADMIN))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("403");
    }

    private Inspection inspection(Long facilityId, String facilityName, String regionCode,
                                  String regionName, DefectType defectType, Severity severity) {
        return new Inspection(facilityId, facilityName, regionCode, regionName, 1L,
                "/photo.jpg", "벽면", null,
                new DefectAnalysis(defectType, severity, 0.8, List.of("확인"), List.of(), "요약"));
    }
}
