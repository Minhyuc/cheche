package com.cheche.facility.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

import com.cheche.facility.domain.AdminRole;
import com.cheche.facility.dto.FacilityRequest;
import com.cheche.facility.repository.FacilityRepository;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class FacilityServiceTest {
    private final FacilityService service = new FacilityService(mock(FacilityRepository.class));

    @Test
    void regionalAdminCannotCreateOutsideAssignedRegion() {
        FacilityRequest request = new FacilityRequest("송파체육관", "체육관", "11710", "서울 송파구",
                "서울 송파구", null, null, null);

        assertThrows(ResponseStatusException.class,
                () -> service.create(request, 1001L, AdminRole.REGIONAL_ADMIN, "11680"));
    }
}
