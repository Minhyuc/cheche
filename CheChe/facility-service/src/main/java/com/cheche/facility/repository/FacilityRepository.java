package com.cheche.facility.repository;

import com.cheche.facility.domain.Facility;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityRepository extends JpaRepository<Facility, Long> {
    List<Facility> findAllByRegionCodeOrderByNameAsc(String regionCode);
}
