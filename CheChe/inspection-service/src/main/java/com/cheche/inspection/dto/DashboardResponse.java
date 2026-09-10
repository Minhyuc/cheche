package com.cheche.inspection.dto;

public record DashboardResponse(long totalInspections, long unresolvedInspections, long resolvedInspections) {}
