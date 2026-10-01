package com.cheche.login.config;

import com.cheche.login.domain.AccountType;
import com.cheche.login.domain.User;
import com.cheche.login.repository.UserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** MVP 전용 고정 슈퍼관리자 로그인 계정을 보장한다. 운영 전 반드시 제거하거나 비밀값 기반으로 교체한다. */
@Component
public class MvpSuperAdminBootstrap implements ApplicationRunner {
    public static final String USERNAME = "superadmin";
    public static final String PASSWORD = "superadmin";

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;

    public MvpSuperAdminBootstrap(UserRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        User user = repository.findByUsername(USERNAME)
                .orElseGet(() -> new User(USERNAME, passwordEncoder.encode(PASSWORD), AccountType.ADMIN));
        if (user.getAccountType() != AccountType.ADMIN
                || !passwordEncoder.matches(PASSWORD, user.getPasswordHash())) {
            user.configureAsAdmin(passwordEncoder.encode(PASSWORD));
        }
        repository.save(user);
    }
}
