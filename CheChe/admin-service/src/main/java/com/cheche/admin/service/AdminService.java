package com.cheche.admin.service;

import com.cheche.admin.domain.AdminRole;
import com.cheche.admin.domain.Administrator;
import com.cheche.admin.dto.*;
import com.cheche.admin.repository.AdministratorRepository;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminService {
    private static final String MVP_SUPER_ADMIN_USERNAME = "superadmin";
    private final AdministratorRepository repository;

    public AdminService(AdministratorRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public AdminResponse sync(AdminSyncRequest request) {
        Administrator admin = repository.findByUserId(request.userId())
                .orElseGet(() -> new Administrator(request.userId(), request.username(), null));
        admin.syncIdentity(request.username(), admin.getEmail());
        if (MVP_SUPER_ADMIN_USERNAME.equalsIgnoreCase(request.username())) {
            admin.changeRole(AdminRole.SUPER_USER);
            admin.assignRegion(null, null);
        }
        return AdminResponse.from(repository.save(admin));
    }

    @Transactional(readOnly = true)
    public AdminResponse getMe(Long userId) {
        return AdminResponse.from(findByUserId(userId));
    }

    public List<RegionOptionResponse> regions() {
        return SeoulDistricts.ALL.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .map(entry -> new RegionOptionResponse(entry.getKey(), entry.getValue()))
                .toList();
    }

    @Transactional
    public AdminResponse setupMyRegion(Long userId, RegionSetupRequest request) {
        Administrator admin = findByUserId(userId);
        if (admin.getRole() == AdminRole.SUPER_USER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "슈퍼유저는 담당 지역 설정이 필요하지 않습니다.");
        }
        if (admin.getRegionCode() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "담당 지역은 이미 설정되었습니다. 변경은 슈퍼유저에게 요청하세요.");
        }
        admin.assignRegion(request.regionCode(), requireSeoulRegion(request.regionCode()));
        return AdminResponse.from(admin);
    }

    @Transactional(readOnly = true)
    public List<AdminResponse> listAll(AdminRole requesterRole) {
        requireSuperUser(requesterRole);
        return repository.findAll().stream().map(AdminResponse::from).toList();
    }

    @Transactional
    public AdminResponse updateAuthority(Long id, AdminAuthorityRequest request, AdminRole requesterRole) {
        requireSuperUser(requesterRole);
        Administrator admin = repository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "관리자를 찾을 수 없습니다."));
        admin.changeRole(request.role());
        admin.changeStatus(request.status());
        if (request.role() == AdminRole.SUPER_USER) {
            admin.assignRegion(null, null);
        } else {
            String regionCode = request.regionCode() != null ? request.regionCode() : admin.getRegionCode();
            admin.assignRegion(regionCode, requireSeoulRegion(regionCode));
        }
        return AdminResponse.from(admin);
    }

    private Administrator findByUserId(Long userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "관리자 정보가 없습니다. /sync를 먼저 호출하세요."));
    }

    private void requireSuperUser(AdminRole role) {
        if (role != AdminRole.SUPER_USER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "슈퍼유저 권한이 필요합니다.");
        }
    }

    private String requireSeoulRegion(String regionCode) {
        String regionName = SeoulDistricts.ALL.get(regionCode);
        if (regionName == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "서울특별시 25개 자치구 코드만 설정할 수 있습니다.");
        }
        return regionName;
    }
}
