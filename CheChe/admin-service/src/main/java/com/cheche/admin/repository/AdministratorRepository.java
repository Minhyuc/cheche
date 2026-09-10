package com.cheche.admin.repository;

import com.cheche.admin.domain.Administrator;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AdministratorRepository extends JpaRepository<Administrator, Long> {
    Optional<Administrator> findByUserId(Long userId);
}
