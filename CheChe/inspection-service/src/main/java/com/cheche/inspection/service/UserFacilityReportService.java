package com.cheche.inspection.service;

import com.cheche.inspection.domain.FacilityReport;
import com.cheche.inspection.domain.ReportCategory;
import com.cheche.inspection.dto.FacilityReportResponse;
import com.cheche.inspection.dto.FacilitySummary;
import com.cheche.inspection.repository.FacilityReportRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserFacilityReportService {
    private final FacilityReportRepository repository;
    private final PhotoStorage photoStorage;
    private final FacilityServiceClient facilityClient;

    public UserFacilityReportService(FacilityReportRepository repository, PhotoStorage photoStorage,
                                     FacilityServiceClient facilityClient) {
        this.repository = repository;
        this.photoStorage = photoStorage;
        this.facilityClient = facilityClient;
    }

    @Transactional
    public FacilityReportResponse create(Long userId, String userRegionCode, Long facilityId,
                                         ReportCategory category, String locationDescription,
                                         String comment, MultipartFile photo) {
        requireText(locationDescription, "시설 내 위치를 입력해 주세요.", 240);
        requireText(comment, "개선이 필요한 내용을 입력해 주세요.", 2000);
        FacilitySummary facility = facilityClient.get(facilityId, userId, userRegionCode);
        String photoUrl = photoStorage.store(photo);
        FacilityReport report = new FacilityReport(userId, facility.id(), facility.name(),
                userRegionCode, facility.regionName(), category, locationDescription.trim(),
                comment.trim(), photoUrl);
        return FacilityReportResponse.from(repository.save(report));
    }

    @Transactional(readOnly = true)
    public List<FacilityReportResponse> mine(Long userId) {
        return repository.findAllByReporterUserIdOrderByCreatedAtDesc(userId).stream()
                .map(FacilityReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public FacilityReportResponse getMine(Long userId, Long reportId) {
        FacilityReport report = repository.findById(reportId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "시설 개선 요청을 찾을 수 없습니다."));
        if (!report.getReporterUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "본인이 작성한 요청만 확인할 수 있습니다.");
        }
        return FacilityReportResponse.from(report);
    }

    private void requireText(String value, String message, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        if (value.length() > maxLength) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, maxLength + "자 이내로 입력해 주세요.");
        }
    }
}
