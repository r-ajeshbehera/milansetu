package com.rajesh.milansetu.dto.response;

import java.util.List;

/** A ranked suggestion with the reasons, so desk staff understand WHY it scored. Never auto-confirmed. */
public record MatchSuggestion(Long foundReportId, Long foundCaseId, int score, List<Factor> factors,
                              double distanceKm, long minutesApart, FoundReportResponse found) {
    public record Factor(String factor, int points, String detail) { }
}
