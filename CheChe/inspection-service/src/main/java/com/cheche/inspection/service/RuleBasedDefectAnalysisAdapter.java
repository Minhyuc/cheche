package com.cheche.inspection.service;

import com.cheche.inspection.domain.*;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Temporary adapter. Replace only this component when the AI vision service is connected. */
@Component
public class RuleBasedDefectAnalysisAdapter implements DefectAnalysisPort {
    @Override
    public DefectAnalysis analyze(MultipartFile photo, String suspectedDefect,
                                  String locationDescription, String note) {
        String text = String.join(" ", safe(suspectedDefect), safe(locationDescription), safe(note)).toLowerCase(Locale.ROOT);
        DefectType type = classify(text);
        Severity severity = text.contains("붕괴") || text.contains("파손") ? Severity.HIGH
                : text.contains("진행") || text.contains("넓") ? Severity.MEDIUM : Severity.LOW;
        List<String> checklist = checklist(type);
        List<String> similar = List.of(
                "동일 시설의 최근 " + label(type) + " 기록 확인",
                "동일 지역·동일 결함 유형의 조치 완료 사례 비교"
        );
        String report = "%s에서 %s 의심 결함이 접수되었습니다. 위험도는 %s이며 현장 치수 측정과 주변부 추가 촬영이 필요합니다."
                .formatted(locationDescription, label(type), severity.name());
        return new DefectAnalysis(type, severity, 0.65, checklist, similar, report);
    }

    private DefectType classify(String text) {
        if (text.contains("균열") || text.contains("갈라")) return DefectType.CRACK;
        if (text.contains("부식") || text.contains("녹")) return DefectType.CORROSION;
        if (text.contains("누수") || text.contains("물")) return DefectType.WATER_LEAK;
        if (text.contains("변형") || text.contains("휘어")) return DefectType.DEFORMATION;
        if (text.contains("박리") || text.contains("표면")) return DefectType.SURFACE_DAMAGE;
        return DefectType.OTHER;
    }

    private List<String> checklist(DefectType type) {
        return switch (type) {
            case CRACK -> List.of("균열 폭·길이 측정", "진행성 표시 및 재촬영", "주변 누수·박리 확인");
            case CORROSION -> List.of("부식 면적 확인", "단면 손실 여부 확인", "습기·염분 원인 확인");
            case WATER_LEAK -> List.of("누수 시작점 추적", "전기 설비 접근 통제", "배수 상태 확인");
            default -> List.of("손상 범위 측정", "이용자 접근 통제 필요성 판단", "전문 점검 요청 여부 결정");
        };
    }

    private String label(DefectType type) {
        return switch (type) {
            case CRACK -> "균열";
            case CORROSION -> "부식";
            case DEFORMATION -> "변형";
            case SURFACE_DAMAGE -> "표면 손상";
            case WATER_LEAK -> "누수";
            case OTHER -> "기타 손상";
        };
    }

    private String safe(String value) { return value == null ? "" : value; }
}
