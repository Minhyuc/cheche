package com.cheche.login.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.cheche.login.client.AdminServiceClient;
import com.cheche.login.domain.User;
import com.cheche.login.domain.AccountType;
import com.cheche.login.dto.*;
import com.cheche.login.repository.UserRepository;
import com.cheche.login.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

class LoginServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final JwtService jwtService = mock(JwtService.class);
    private final AdminServiceClient adminClient = mock(AdminServiceClient.class);
    private final LoginService service = new LoginService(repository, encoder, jwtService, adminClient);

    @Test
    void loginReturnsInitialSetupFlagFromAdminDatabase() {
        User user = new User("regional-admin", "encoded");
        ReflectionTestUtils.setField(user, "id", 10L);
        when(repository.findByUsername("regional-admin")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "encoded")).thenReturn(true);
        when(adminClient.sync(10L, "regional-admin"))
                .thenReturn(new AdminProfile(10L, "REGIONAL_ADMIN", "ACTIVE", null, null, true));
        when(jwtService.issue(user)).thenReturn("signed.jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = service.loginAdmin(new CredentialsRequest("regional-admin", "password123"));

        assertTrue(response.initialSetupRequired());
        assertEquals("signed.jwt", response.accessToken());
        assertNull(response.regionCode());
    }

    @Test
    void userLoginDoesNotSynchronizeAdminProfile() {
        User user = new User("sports-user", "encoded", AccountType.USER);
        ReflectionTestUtils.setField(user, "id", 20L);
        when(repository.findByUsername("sports-user")).thenReturn(Optional.of(user));
        when(encoder.matches("password123", "encoded")).thenReturn(true);
        when(jwtService.issue(user)).thenReturn("user.jwt");
        when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        UserLoginResponse response = service.loginUser(new CredentialsRequest("sports-user", "password123"));

        assertEquals("USER", response.accountType());
        assertEquals("user.jwt", response.accessToken());
        verifyNoInteractions(adminClient);
    }
}
