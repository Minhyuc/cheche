package com.cheche.login.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.cheche.login.domain.AccountType;
import com.cheche.login.domain.User;
import com.cheche.login.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.security.crypto.password.PasswordEncoder;

class MvpSuperAdminBootstrapTest {
    @Test
    void createsFixedAdminAccountWhenMissing() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        when(repository.findByUsername("superadmin")).thenReturn(Optional.empty());
        when(encoder.encode("superadmin")).thenReturn("encoded");

        new MvpSuperAdminBootstrap(repository, encoder)
                .run(new DefaultApplicationArguments(new String[0]));

        verify(repository).save(argThat(user -> user.getUsername().equals("superadmin")
                && user.getAccountType() == AccountType.ADMIN
                && user.getPasswordHash().equals("encoded")));
    }

    @Test
    void repairsExistingAccountToFixedAdminCredentials() throws Exception {
        UserRepository repository = mock(UserRepository.class);
        PasswordEncoder encoder = mock(PasswordEncoder.class);
        User existing = new User("superadmin", "old", AccountType.USER);
        when(repository.findByUsername("superadmin")).thenReturn(Optional.of(existing));
        when(encoder.encode("superadmin")).thenReturn("new-encoded");

        new MvpSuperAdminBootstrap(repository, encoder)
                .run(new DefaultApplicationArguments(new String[0]));

        assertEquals(AccountType.ADMIN, existing.getAccountType());
        assertEquals("new-encoded", existing.getPasswordHash());
        verify(repository).save(existing);
    }
}
