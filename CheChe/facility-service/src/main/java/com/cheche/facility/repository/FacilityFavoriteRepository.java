package com.cheche.facility.repository;

import com.cheche.facility.domain.FacilityFavorite;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityFavoriteRepository extends JpaRepository<FacilityFavorite, Long> {
    boolean existsByUserIdAndFacilityId(Long userId, Long facilityId);
    void deleteByUserIdAndFacilityId(Long userId, Long facilityId);
    List<FacilityFavorite> findAllByUserId(Long userId);
}
