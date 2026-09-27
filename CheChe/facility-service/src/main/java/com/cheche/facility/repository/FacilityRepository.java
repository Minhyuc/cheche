package com.cheche.facility.repository;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findAllByRegionCodeOrderByNameAsc(String regionCode);
    List<Facility> findAllByStatusOrderByNameAsc(FacilityStatus status);
    List<Facility> findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus status, String regionCode);
    List<Facility> findAllBySourceAndRegionCode(String source, String regionCode);
    List<Facility> findAllBySource(String source);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from Facility f where f.id = :id")
    Optional<Facility> findByIdForUpdate(@Param("id") Long id);
}
