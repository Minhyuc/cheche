package com.cheche.inspection.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import com.cheche.inspection.domain.DefectAnalysis;
import com.cheche.inspection.domain.DefectType;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

class RuleBasedDefectAnalysisAdapterTest {
    private final RuleBasedDefectAnalysisAdapter adapter = new RuleBasedDefectAnalysisAdapter();

    @Test
    void classifiesCrackAndProvidesChecklist() {
        DefectAnalysis result = adapter.analyze(mock(MultipartFile.class), "균열 의심", "관중석 벽면", "진행 중");

        assertEquals(DefectType.CRACK, result.defectType());
        assertFalse(result.checklist().isEmpty());
        assertFalse(result.reportSummary().isBlank());
    }
}
