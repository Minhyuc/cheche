package com.cheche.inspection.repository;

import com.cheche.inspection.domain.FacilityReport;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FacilityReportRepository extends JpaRepository<FacilityReport, Long> {
    List<FacilityReport> findAllByReporterUserIdOrderByCreatedAtDesc(Long reporterUserId);
}
