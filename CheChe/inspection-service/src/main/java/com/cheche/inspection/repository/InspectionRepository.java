package com.cheche.inspection.repository;

import com.cheche.inspection.domain.ActionStatus;
import com.cheche.inspection.domain.Inspection;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InspectionRepository extends JpaRepository<Inspection, Long> {
    List<Inspection> findAllByRegionCodeOrderByCreatedAtDesc(String regionCode);
    List<Inspection> findAllByFacilityIdOrderByCreatedAtDesc(Long facilityId);
    List<Inspection> findAllByFacilityIdAndRegionCodeOrderByCreatedAtDesc(Long facilityId, String regionCode);
    List<Inspection> findAllByActionStatusInOrderByCreatedAtDesc(Collection<ActionStatus> statuses);
    List<Inspection> findAllByRegionCodeAndActionStatusInOrderByCreatedAtDesc(String regionCode, Collection<ActionStatus> statuses);
    long countByRegionCode(String regionCode);
    long countByRegionCodeAndActionStatusIn(String regionCode, Collection<ActionStatus> statuses);
    long countByActionStatusIn(Collection<ActionStatus> statuses);
}
