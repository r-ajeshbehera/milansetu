package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.response.DashboardSummary;
import com.rajesh.milansetu.entity.CaseFile;
import com.rajesh.milansetu.entity.CaseStatus;
import com.rajesh.milansetu.entity.Event;
import com.rajesh.milansetu.entity.Role;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.repository.CaseFileRepository;
import com.rajesh.milansetu.repository.EventRepository;
import com.rajesh.milansetu.repository.FoundReportRepository;
import com.rajesh.milansetu.repository.MissingReportRepository;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/** Every number comes from a database query; nothing is hard-coded. */
@Service
public class DashboardService {
    private final CaseFileRepository cases;
    private final MissingReportRepository missing;
    private final FoundReportRepository found;
    private final EventRepository events;
    private final AccessPolicy access;

    public DashboardService(CaseFileRepository cases, MissingReportRepository missing, FoundReportRepository found,
                            EventRepository events, AccessPolicy access) {
        this.cases = cases;
        this.missing = missing;
        this.found = found;
        this.events = events;
        this.access = access;
    }

    @Transactional(readOnly = true)
    public DashboardSummary summary(Long eventId, LocalDate from, LocalDate to, AppUserPrincipal user) {
        Long id = eventId != null ? eventId : defaultEvent(user);
        access.assertEventRole(user, id, Role.POLICE, Role.ADMIN);
        LocalDateTime fromDt = from == null ? null : from.atStartOfDay();
        LocalDateTime toDt = to == null ? null : to.plusDays(1).atStartOfDay();

        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (CaseStatus s : CaseStatus.values()) byStatus.put(s.name(), 0L);
        for (Object[] row : cases.countByStatus(id)) byStatus.put(((CaseStatus) row[0]).name(), (Long) row[1]);
        long open = byStatus.get("REPORTED") + byStatus.get("MATCHED") + byStatus.get("AT_DESK");

        List<CaseFile> reunited = cases.findReunited(id);
        LocalDateTime today = LocalDate.now().atStartOfDay();
        long reunitedToday = reunited.stream().filter(c -> !c.getReunitedAt().isBefore(today)).count();
        Double avg = reunited.isEmpty() ? null : reunited.stream()
                .mapToLong(c -> Duration.between(c.getOpenedAt(), c.getReunitedAt()).toMinutes()).average().orElse(0);

        List<CaseFile> opened = cases.findOpenedSince(id, fromDt);
        long stale = opened.stream().filter(c -> c.getStatus().isOpen() && c.getStaleFlaggedAt() != null).count();

        TreeMap<String, long[]> trend = new TreeMap<>();
        for (CaseFile c : opened) {
            if (toDt != null && !c.getOpenedAt().isBefore(toDt)) continue;
            trend.computeIfAbsent(c.getOpenedAt().toLocalDate().toString(), k -> new long[2])[0]++;
        }
        for (CaseFile c : reunited) {
            if ((fromDt == null || !c.getReunitedAt().isBefore(fromDt)) && (toDt == null || c.getReunitedAt().isBefore(toDt))) {
                trend.computeIfAbsent(c.getReunitedAt().toLocalDate().toString(), k -> new long[2])[1]++;
            }
        }
        List<DashboardSummary.DeskCount> byDesk = found.countByDesk(id).stream()
                .map(r -> new DashboardSummary.DeskCount((String) r[0], (Long) r[1])).toList();

        return new DashboardSummary(id, open, reunitedToday, missing.countForEvent(id, fromDt, toDt),
                found.countForEvent(id, fromDt, toDt), byStatus.get("REPORTED"),
                byStatus.get("MATCHED") + byStatus.get("AT_DESK"), avg == null ? null : Math.round(avg * 10) / 10.0,
                stale, byStatus, byDesk,
                trend.entrySet().stream().map(e -> new DashboardSummary.TrendPoint(e.getKey(), e.getValue()[0], e.getValue()[1])).toList());
    }

    private Long defaultEvent(AppUserPrincipal user) {
        List<Long> mine = access.staffEvents(user);
        return events.findByActiveTrueOrderByStartDateDesc().stream().map(Event::getId)
                .filter(e -> mine == null || mine.contains(e)).findFirst()
                .orElseThrow(() -> new BusinessRuleException("NO_EVENT", "No active event for this user; pass eventId"));
    }
}
