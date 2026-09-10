package com.cheche.facility.service;

import com.cheche.facility.domain.AdminRole;
import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import com.cheche.facility.dto.FacilityRequest;
import com.cheche.facility.dto.FacilityResponse;
import com.cheche.facility.repository.FacilityRepository;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FacilityService {
    private final FacilityRepository repository;

    public FacilityService(FacilityRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<FacilityResponse> list(AdminRole role, String regionCode) {
        List<Facility> facilities = role == AdminRole.SUPER_USER
                ? repository.findAll()
                : repository.findAllByRegionCodeOrderByNameAsc(requireRegion(regionCode));
        return facilities.stream().map(FacilityResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public FacilityResponse get(Long id, AdminRole role, String regionCode) {
        Facility facility = find(id);
        verifyScope(facility, role, regionCode);
        return FacilityResponse.from(facility);
    }

    @Transactional
    public FacilityResponse create(FacilityRequest request, Long userId, AdminRole role, String regionCode) {
        if (role != AdminRole.SUPER_USER && !request.regionCode().equals(requireRegion(regionCode))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 지역의 시설만 등록할 수 있습니다.");
        }
        Facility facility = new Facility(request.name(), request.type(), request.regionCode(),
                request.regionName(), request.address(), request.phone(), userId, request.publicNotice());
        if (request.status() != null && request.status() != FacilityStatus.OPERATING) {
            facility.update(request.name(), request.type(), request.address(), request.phone(),
                    request.status(), request.publicNotice());
        }
        return FacilityResponse.from(repository.save(facility));
    }

    @Transactional
    public FacilityResponse update(Long id, FacilityRequest request, AdminRole role, String regionCode) {
        Facility facility = find(id);
        verifyScope(facility, role, regionCode);
        facility.update(request.name(), request.type(), request.address(), request.phone(),
                request.status() == null ? facility.getStatus() : request.status(), request.publicNotice());
        return FacilityResponse.from(facility);
    }

    private Facility find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "체육시설을 찾을 수 없습니다."));
    }

    private void verifyScope(Facility facility, AdminRole role, String regionCode) {
        if (role != AdminRole.SUPER_USER && !facility.getRegionCode().equals(requireRegion(regionCode))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "담당 지역 밖의 시설입니다.");
        }
    }

    private String requireRegion(String regionCode) {
        if (regionCode == null || regionCode.isBlank()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "최초 로그인 지역 설정이 필요합니다.");
        }
        return regionCode;
    }
}
