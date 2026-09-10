package com.cheche.inspection.domain;

import java.util.List;

public record DefectAnalysis(
        DefectType defectType,
        Severity severity,
        double confidence,
        List<String> checklist,
        List<String> similarCases,
        String reportSummary
) {}
