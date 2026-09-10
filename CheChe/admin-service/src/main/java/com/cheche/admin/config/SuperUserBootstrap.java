package com.cheche.admin.config;

import com.cheche.admin.domain.AdminRole;
import com.cheche.admin.domain.Administrator;
import com.cheche.admin.repository.AdministratorRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Creates or promotes the first superuser only when explicitly configured. */
@Component
public class SuperUserBootstrap implements ApplicationRunner {
    private final AdministratorRepository repository;
    private final String configuredUserId;
    private final String name;
    private final String email;

    public SuperUserBootstrap(
            AdministratorRepository repository,
            @Value("${cheche.bootstrap-super-user.id:}") String configuredUserId,
            @Value("${cheche.bootstrap-super-user.name:CheChe Super User}") String name,
            @Value("${cheche.bootstrap-super-user.email:superuser@cheche.local}") String email) {
        this.repository = repository;
        this.configuredUserId = configuredUserId;
        this.name = name;
        this.email = email;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (configuredUserId == null || configuredUserId.isBlank()) return;
        Long userId = Long.valueOf(configuredUserId);
        Administrator admin = repository.findByUserId(userId)
                .orElseGet(() -> new Administrator(userId, name, email));
        admin.syncIdentity(name, email);
        admin.changeRole(AdminRole.SUPER_USER);
        admin.assignRegion(null, null);
        repository.save(admin);
    }
}
