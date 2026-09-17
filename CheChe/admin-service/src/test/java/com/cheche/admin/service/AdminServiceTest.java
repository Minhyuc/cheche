package com.cheche.admin.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cheche.admin.domain.Administrator;
import com.cheche.admin.dto.AdminResponse;
import com.cheche.admin.dto.AdminSyncRequest;
import com.cheche.admin.dto.RegionSetupRequest;
import com.cheche.admin.repository.AdministratorRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

class AdminServiceTest {
    private final AdministratorRepository repository = mock(AdministratorRepository.class);
    private final AdminService service = new AdminService(repository);

    @Test
    void newAdminRequiresInitialRegionSetup() {
        when(repository.findByUserId(1001L)).thenReturn(Optional.empty());
        when(repository.save(any(Administrator.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AdminResponse response = service.sync(new AdminSyncRequest(1001L, "seoul-admin"));

        assertTrue(response.initialSetupRequired());
        assertEquals(1001L, response.userId());
    }

    @Test
    void regionSetupCompletesOnboarding() {
        Administrator admin = new Administrator(1001L, "서울 관리자", "admin@example.com");
        when(repository.findByUserId(1001L)).thenReturn(Optional.of(admin));

        AdminResponse response = service.setupMyRegion(1001L, new RegionSetupRequest("11680", "서울특별시 강남구"));

        assertFalse(response.initialSetupRequired());
        assertEquals("11680", response.regionCode());
        assertEquals("서울특별시 강남구", response.regionName());
    }

    @Test
    void regionSetupRejectsOutsideSeoul() {
        Administrator admin = new Administrator(1001L, "부산 관리자", "admin@example.com");
        when(repository.findByUserId(1001L)).thenReturn(Optional.of(admin));

        assertThrows(ResponseStatusException.class,
                () -> service.setupMyRegion(1001L, new RegionSetupRequest("26110", "부산광역시 중구")));
    }
}
