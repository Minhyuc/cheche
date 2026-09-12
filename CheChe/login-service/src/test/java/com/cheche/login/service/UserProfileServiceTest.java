package com.cheche.login.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cheche.login.domain.AccountType;
import com.cheche.login.domain.User;
import com.cheche.login.dto.RegionUpdateRequest;
import com.cheche.login.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

class UserProfileServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserProfileService service = new UserProfileService(repository);

    @Test
    void savesCanonicalSeoulDistrict() {
        User user = new User("sports-user", "encoded", AccountType.USER);
        ReflectionTestUtils.setField(user, "id", 20L);
        when(repository.findById(20L)).thenReturn(Optional.of(user));

        var response = service.updateRegion(20L, new RegionUpdateRequest("11680"));

        assertEquals("서울특별시 강남구", response.regionName());
        assertFalse(response.initialSetupRequired());
    }

    @Test
    void rejectsRegionOutsideSeoul() {
        User user = new User("sports-user", "encoded", AccountType.USER);
        when(repository.findById(20L)).thenReturn(Optional.of(user));

        assertThrows(ResponseStatusException.class,
                () -> service.updateRegion(20L, new RegionUpdateRequest("26110")));
    }
}
