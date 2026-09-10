package com.cheche.inspection.service;

import com.cheche.inspection.domain.*;
import com.cheche.inspection.dto.*;
import com.cheche.inspection.repository.InspectionRepository;
import java.util.List;
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
                접수 일시: %s
                """.formatted(value.getFacilityName(), value.getFacilityId(), value.getRegionName(),
                value.getLocationDescription(), value.getDefectType(), value.getSeverity(),
                value.getConfidence() * 100, value.getReportSummary(), value.getChecklist(),
                value.getActionStatus(), value.getActionNote() == null ? "미입력" : value.getActionNote(),
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
}
