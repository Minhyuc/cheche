package com.cheche.facility.repository;

import com.cheche.facility.domain.Facility;
import com.cheche.facility.domain.FacilityStatus;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findAllByRegionCodeOrderByNameAsc(String regionCode);
    List<Facility> findAllByStatusOrderByNameAsc(FacilityStatus status);
    List<Facility> findAllByStatusAndRegionCodeOrderByNameAsc(FacilityStatus status, String regionCode);
}
