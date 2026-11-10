package com.rajesh.milansetu.dto.response;

import java.util.List;
import java.util.Map;

public record DashboardSummary(Long eventId, long openCases, long reunitedToday, long totalMissing, long totalFound,
                               long awaitingConfirmation, long awaitingHandover, Double avgMinutesToReunite,
                               long staleCases, Map<String, Long> casesByStatus, List<DeskCount> byDesk,
                               List<TrendPoint> trend) {
    public record DeskCount(String desk, long foundReports) { }
    public record TrendPoint(String date, long opened, long reunited) { }
}
