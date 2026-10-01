package com.cheche.inspection.service;

import com.cheche.inspection.domain.*;
import com.cheche.inspection.dto.*;
import com.cheche.inspection.repository.InspectionRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InspectionService {
    private static final List<ActionStatus> OPEN_STATUSES = List.of(
            ActionStatus.REPORTED, ActionStatus.REVIEWING, ActionStatus.ACTION_SCHEDULED);

    private final InspectionRepository repository;
    private final PhotoStorage photoStorage;
    private final DefectAnalysisPort analysisPort;

    public InspectionService(InspectionRepository repository, PhotoStorage photoStorage,
                             DefectAnalysisPort analysisPort) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.analysisPort = analysisPort;
    }

    @Transactional
    public InspectionResponse create(Long facilityId, String facilityName, String facilityRegionCode,
                                     String facilityRegionName, String locationDescription, String note,
                                     String suspectedDefect, MultipartFile photo, Long userId,
                                     AdminRole role, String adminRegionCode) {
        verifyRegion(facilityRegionCode, role, adminRegionCode);
        String photoUrl = photoStorage.store(photo);
        DefectAnalysis analysis = analysisPort.analyze(photo, suspectedDefect, locationDescription, note);
        Inspection inspection = new Inspection(facilityId, facilityName, facilityRegionCode,
                facilityRegionName, userId, photoUrl, locationDescription, note, analysis);
        return InspectionResponse.from(repository.save(inspection));
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> list(AdminRole role, String regionCode) {
        List<Inspection> values = role == AdminRole.SUPER_USER
                ? repository.findAll()
                : repository.findAllByRegionCodeOrderByCreatedAtDesc(requireRegion(regionCode));
        return values.stream().map(InspectionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> facilityHistory(Long facilityId, AdminRole role, String regionCode) {
        List<Inspection> values = role == AdminRole.SUPER_USER
                ? repository.findAllByFacilityIdOrderByCreatedAtDesc(facilityId)
                : repository.findAllByFacilityIdAndRegionCodeOrderByCreatedAtDesc(facilityId, requireRegion(regionCode));
        return values.stream().map(InspectionResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public List<InspectionResponse> open(AdminRole role, String regionCode) {
        List<Inspection> values = role == AdminRole.SUPER_USER
                ? repository.findAllByActionStatusInOrderByCreatedAtDesc(OPEN_STATUSES)
                : repository.findAllByRegionCodeAndActionStatusInOrderByCreatedAtDesc(requireRegion(regionCode), OPEN_STATUSES);
        return values.stream().map(InspectionResponse::from).toList();
    }

    @Transactional
    public InspectionResponse updateAction(Long id, ActionUpdateRequest request,
                                           AdminRole role, String regionCode) {
        Inspection inspection = find(id);
        verifyRegion(inspection.getRegionCode(), role, regionCode);
        inspection.updateAction(request.status(), request.actionNote());
        return InspectionResponse.from(inspection);
    }

    @Transactional
    public InspectionResponse confirm(Long id, DefectConfirmationRequest request, Long userId,
                                      AdminRole role, String regionCode) {
        Inspection inspection = find(id);
        verifyRegion(inspection.getRegionCode(), role, regionCode);
        if (Boolean.TRUE.equals(request.actionRequired())
                && (request.actionDueDate() == null || request.actionDueDate().isBefore(LocalDate.now()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "조치가 필요하면 오늘 이후의 조치 기한을 입력해야 합니다.");
        }
        inspection.confirm(request.defectType(), request.severity(), request.locationDescription(),
                request.detail(), request.actionRequired(), request.actionDueDate(), userId);
        return InspectionResponse.from(inspection);
    }

    @Transactional(readOnly = true)
    public List<RegionalSafetySummaryResponse> regionalSafety(AdminRole role) {
        requireSuperUser(role);
        Map<String, List<Inspection>> byRegion = new LinkedHashMap<>();
        repository.findAll().forEach(value ->
                byRegion.computeIfAbsent(value.getRegionCode(), ignored -> new ArrayList<>()).add(value));

        return byRegion.values().stream().map(values -> {
            Inspection first = values.get(0);
            long open = values.stream().filter(this::isOpen).count();
            long resolved = values.stream().filter(value -> value.getActionStatus() == ActionStatus.RESOLVED).count();
            long highRiskOpen = values.stream()
                    .filter(this::isOpen)
                    .filter(value -> value.getSeverity() == Severity.HIGH || value.getSeverity() == Severity.CRITICAL)
                    .count();
            int penalty = values.stream().filter(this::isOpen).mapToInt(this::severityPenalty).sum();
            return new RegionalSafetySummaryResponse(first.getRegionCode(), first.getRegionName(),
                    values.stream().map(Inspection::getFacilityId).distinct().count(), values.size(),
                    open, resolved, highRiskOpen, Math.max(0, 100 - penalty));
        }).sorted(Comparator.comparing(RegionalSafetySummaryResponse::regionName)).toList();
    }

    @Transactional(readOnly = true)
    public List<RecurringDefectResponse> recurringDefects(AdminRole role, int minimumOccurrences) {
        requireSuperUser(role);
        if (minimumOccurrences < 2 || minimumOccurrences > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "minimumOccurrences는 2 이상 100 이하여야 합니다.");
        }
        Map<RecurringKey, List<Inspection>> groups = new LinkedHashMap<>();
        repository.findAll().forEach(value -> groups
                .computeIfAbsent(new RecurringKey(value.getFacilityId(), value.getDefectType()),
                        ignored -> new ArrayList<>()).add(value));

        Comparator<LocalDateTime> nullSafeDate = Comparator.nullsLast(Comparator.reverseOrder());
        return groups.values().stream()
                .filter(values -> values.size() >= minimumOccurrences)
                .map(values -> {
                    Inspection first = values.get(0);
                    Severity highest = values.stream().map(Inspection::getSeverity)
                            .max(Comparator.comparingInt(Enum::ordinal)).orElse(Severity.LOW);
                    LocalDateTime lastDetected = values.stream().map(Inspection::getCreatedAt)
                            .filter(value -> value != null).max(Comparator.naturalOrder()).orElse(null);
                    return new RecurringDefectResponse(first.getFacilityId(), first.getFacilityName(),
                            first.getRegionCode(), first.getRegionName(), first.getDefectType(), values.size(),
                            values.stream().filter(this::isOpen).count(), highest, lastDetected);
                })
                .sorted(Comparator.comparingLong(RecurringDefectResponse::occurrenceCount).reversed()
                        .thenComparing(RecurringDefectResponse::lastDetectedAt, nullSafeDate))
                .toList();
    }

    @Transactional(readOnly = true)
    public DashboardResponse dashboard(AdminRole role, String regionCode) {
        long total;
        long unresolved;
        if (role == AdminRole.SUPER_USER) {
            total = repository.count();
            unresolved = repository.countByActionStatusIn(OPEN_STATUSES);
        } else {
            String scopedRegion = requireRegion(regionCode);
            total = repository.countByRegionCode(scopedRegion);
            unresolved = repository.countByRegionCodeAndActionStatusIn(scopedRegion, OPEN_STATUSES);
        }
        return new DashboardResponse(total, unresolved, total - unresolved);
    }

    @Transactional(readOnly = true)
    public String report(Long id, AdminRole role, String regionCode) {
        Inspection value = find(id);
        verifyRegion(value.getRegionCode(), role, regionCode);
        return """
                [CheChe 체육시설 안전점검 보고서]
                시설: %s (#%d)
                지역: %s
                위치: %s
                결함 후보: %s
                위험도: %s
                AI 신뢰도: %.0f%%
                점검 요약: %s
                점검 항목:
                %s
                조치 상태: %s
                조치 내용: %s
                분석 확정: %s
                확정 상세: %s
                조치 필요: %s
                조치 기한: %s
                접수 일시: %s
                """.formatted(value.getFacilityName(), value.getFacilityId(), value.getRegionName(),
                value.getLocationDescription(), value.getDefectType(), value.getSeverity(),
                value.getConfidence() * 100, value.getReportSummary(), value.getChecklist(),
                value.getActionStatus(), value.getActionNote() == null ? "미입력" : value.getActionNote(),
                value.isConfirmed() ? "확정" : "미확정",
                value.getConfirmedDetail() == null ? "미입력" : value.getConfirmedDetail(),
                value.getActionRequired() == null ? "미확정" : value.getActionRequired() ? "필요" : "불필요",
                value.getActionDueDate() == null ? "해당 없음" : value.getActionDueDate(),
                value.getCreatedAt());
    }

    @Transactional(readOnly = true)
    public List<PublicInspectionStatusResponse> publicFacilityStatus(Long facilityId) {
        return repository.findAllByFacilityIdOrderByCreatedAtDesc(facilityId).stream()
                .map(PublicInspectionStatusResponse::from)
                .toList();
    }

    private Inspection find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "점검 기록을 찾을 수 없습니다."));
    }

    private void verifyRegion(String targetRegion, AdminRole role, String adminRegion) {
        if (role != AdminRole.SUPER_USER && !targetRegion.equals(requireRegion(adminRegion))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 지역 밖의 점검 기록입니다.");
        }
    }

    private String requireRegion(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "최초 로그인 지역 설정이 필요합니다.");
        }
        return regionCode;
    }

    private void requireSuperUser(AdminRole role) {
        if (role != AdminRole.SUPER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "슈퍼관리자만 조회할 수 있습니다.");
        }
    }

    private boolean isOpen(Inspection value) {
        return OPEN_STATUSES.contains(value.getActionStatus());
    }

    private int severityPenalty(Inspection value) {
        return switch (value.getSeverity()) {
            case LOW -> 2;
            case MEDIUM -> 5;
            case HIGH -> 10;
            case CRITICAL -> 20;
        };
    }

    private record RecurringKey(Long facilityId, DefectType defectType) {}
}
