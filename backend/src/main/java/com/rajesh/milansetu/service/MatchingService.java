package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.response.FoundReportResponse;
import com.rajesh.milansetu.dto.response.MatchSuggestion;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.CaseFileRepository;
import com.rajesh.milansetu.repository.FoundReportRepository;
import com.rajesh.milansetu.repository.MissingReportRepository;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;

/**
 * Ranks OPEN found reports of the same event against one missing report.
 * It only SUGGESTS: confirming is a separate, human action, and handover needs an OTP.
 */
@Service
public class MatchingService {
    private final MissingReportRepository missing;
    private final FoundReportRepository found;
    private final CaseFileRepository cases;
    private final AccessPolicy access;

    public MatchingService(MissingReportRepository missing, FoundReportRepository found, CaseFileRepository cases,
                           AccessPolicy access) {
        this.missing = missing;
        this.found = found;
        this.cases = cases;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public List<MatchSuggestion> suggestionsFor(Long missingReportId, AppUserPrincipal user) {
        MissingReport m = missing.findWithDetails(missingReportId)
                .orElseThrow(() -> new ResourceNotFoundException("Missing report", missingReportId));
        access.assertEventRole(user, m.getEventId(), Role.DESK, Role.POLICE);
        if (m.getStatus() != ReportStatus.OPEN) return List.of();
        return rank(m, found.findCandidates(m.getEventId(), ReportStatus.OPEN)).stream()
                .map(s -> new MatchSuggestion(s.foundReportId(), cases.findCanonicalByFound(s.foundReportId()).map(CaseFile::getId).orElse(null),
                        s.score(), s.factors(), s.distanceKm(), s.minutesApart(), s.found()))
                .toList();
    }

    /** Pure ranking step (unit-tested): filter, score, threshold 50, sort by score desc then distance. */
    public static List<MatchSuggestion> rank(MissingReport m, List<FoundReport> candidates) {
        MatchScorer.Person missingPerson = new MatchScorer.Person(m.getGender(), m.getApproxAge(), m.getClothingColor(),
                m.getLastSeenLat().doubleValue(), m.getLastSeenLng().doubleValue(), m.getLastSeenAt());
        return candidates.stream()
                .filter(f -> f.getStatus() == ReportStatus.OPEN && Objects.equals(f.getEventId(), m.getEventId()))
                // two DIFFERENT registered members can never be the same person
                .filter(f -> f.getMember() == null || m.getMember() == null || f.getMember().getId().equals(m.getMember().getId()))
                .flatMap(f -> MatchScorer.score(missingPerson, new MatchScorer.Person(f.getGender(), f.getApproxAge(),
                                f.getClothingColor(), f.getFoundLat().doubleValue(), f.getFoundLng().doubleValue(), f.getFoundAt()))
                        .filter(r -> MatchScorer.isSuggestion(r.score()))
                        .map(r -> new MatchSuggestion(f.getId(), null, r.score(), r.factors(), r.distanceKm(), r.minutesApart(),
                                FoundReportResponse.from(f)))
                        .stream())
                .sorted(Comparator.comparingInt(MatchSuggestion::score).reversed()
                        .thenComparingDouble(MatchSuggestion::distanceKm))
                .toList();
    }
}
