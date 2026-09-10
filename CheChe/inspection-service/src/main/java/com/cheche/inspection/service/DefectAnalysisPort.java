package com.cheche.inspection.service;

import com.cheche.inspection.domain.DefectAnalysis;
import org.springframework.web.multipart.MultipartFile;

public interface DefectAnalysisPort {
    DefectAnalysis analyze(MultipartFile photo, String suspectedDefect, String locationDescription, String note);
}
