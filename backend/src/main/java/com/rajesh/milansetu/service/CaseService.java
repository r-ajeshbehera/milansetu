package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.AssignRequest;
import com.rajesh.milansetu.dto.request.ConfirmMatchRequest;
import com.rajesh.milansetu.dto.response.CaseResponse;
import com.rajesh.milansetu.dto.response.TimelineEntry;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.InvalidStateException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.notification.NotificationService;
import com.rajesh.milansetu.repository.*;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;

@Service
public class CaseService {
    private static final EnumSet<ReportStatus> ACTIVE = EnumSet.of(ReportStatus.OPEN, ReportStatus.MATCHED);

    private final CaseFileRepository cases;
    private final CaseEventRepository caseEvents;
    private final FoundReportRepository found;
    private final MissingReportRepository missing;
    private final HelpDeskRepository desks;
    private final UserRepository users;
    private final EventRepository events;
    private final OtpService otp;
    private final NotificationService notifications;
    private final CaseMapper mapper;
    private final AccessPolicy access;
    private final AuditService audit;

    public CaseService(CaseFileRepository cases, CaseEventRepository caseEvents, FoundReportRepository found,
                       MissingReportRepository missing, HelpDeskRepository desks, UserRepository users, EventRepository events,
                       OtpService otp, NotificationService notifications, CaseMapper mapper, AccessPolicy access,
                       AuditService audit) {
        this.cases = cases;
        this.caseEvents = caseEvents;
        this.found = found;
        this.missing = missing;
        this.desks = desks;
        this.users = users;
        this.events = events;
        this.otp = otp;
        this.notifications = notifications;
        this.mapper = mapper;
        this.access = access;
        this.audit = audit;
    }

    // ---- queries -------------------------------------------------------------------

    @Transactional(readOnly = true)
    public Page<CaseResponse> list(Long eventId, CaseStatus status, Long deskId, LocalDate from, LocalDate to,
                                   Pageable pageable, AppUserPrincipal user) {
        if (user.role() == Role.FAMILY) {
            return cases.searchForFamily(user.id(), pageable).map(c -> mapper.toResponse(c, user));
        }
        List<Long> scope = access.staffEvents(user);
        if (scope == null) scope = events.findAll().stream().map(Event::getId).toList();
        if (eventId != null) {
            if (!scope.contains(eventId)) throw new ResourceNotFoundException("Event", eventId);
            scope = List.of(eventId);
        }
        if (scope.isEmpty()) scope = List.of(-1L);
        return cases.searchForStaff(scope, status, deskId, from == null ? null : from.atStartOfDay(),
                to == null ? null : to.plusDays(1).atStartOfDay(), pageable).map(c -> mapper.toResponse(c, user));
    }

    @Transactional(readOnly = true)
    public CaseResponse get(Long id, AppUserPrincipal user) {
        CaseFile c = loadCanonical(id);
        access.assertCanViewCase(user, c);
        return mapper.toResponse(c, user);
    }

    /** Timeline of the case plus the history of any case merged into it. */
    @Transactional(readOnly = true)
    public List<TimelineEntry> timeline(Long id, AppUserPrincipal user) {
        CaseFile c = loadCanonical(id);
        access.assertCanViewCase(user, c);
        List<Long> ids = new ArrayList<>();
        ids.add(c.getId());
        cases.findMergedInto(c.getId()).forEach(o -> ids.add(o.getId()));   // history of merged-in cases
        boolean family = user.role() == Role.FAMILY;
        return caseEvents.findTimeline(ids).stream()
                .map(e -> new TimelineEntry(e.getCaseId(), e.getFromStatus(), e.getStatus(), e.getNote(),
                        e.getChangedBy() == null ? "system" : family ? roleLabel(e.getChangedBy().getRole()) : e.getChangedBy().getFullName(),
                        e.getChangedAt()))
                .toList();
    }

    // ---- desk actions --------------------------------------------------------------

    /**
     * Human confirmation of a match (never automatic).
     * a) foundReportId given: merge the found-side case into this missing-side case.
     * b) no foundReportId: a QR case for a registered member; the desk confirms identity (and an open missing report
     *    for the same member, if any, is merged in).
     * Then the guardian receives an OTP.
     */
    @Transactional
    public CaseResponse confirmMatch(Long caseId, ConfirmMatchRequest req, AppUserPrincipal desk) {
        CaseFile c = loadCanonical(caseId);
        access.assertEventRole(desk, c.getEventId(), Role.DESK);
        if (c.getStatus() != CaseStatus.REPORTED) throw new InvalidStateException("Only a REPORTED case can be matched");
        User me = users.getReferenceById(desk.id());

        if (req.foundReportId() != null) {
            if (c.getMissingReport() == null || c.getFoundReport() != null) {
                throw new BusinessRuleException("WRONG_CASE_SIDE", "Confirm from the missing-person case");
            }
            FoundReport f = found.findWithDetails(req.foundReportId())
                    .orElseThrow(() -> new ResourceNotFoundException("Found report", req.foundReportId()));
            MissingReport m = c.getMissingReport();
            if (!f.getEventId().equals(c.getEventId()) || f.getStatus() != ReportStatus.OPEN) {
                throw new BusinessRuleException("NOT_MATCHABLE", "Found report is not open in this event");
            }
            if (f.getGender() != m.getGender()) throw new BusinessRuleException("GENDER_MISMATCH", "Gender differs: cannot be the same person");
            if (f.getMember() != null && m.getMember() != null && !f.getMember().getId().equals(m.getMember().getId())) {
                throw new BusinessRuleException("DIFFERENT_MEMBERS", "Two different registered members");
            }
            CaseFile foundCase = cases.findCanonicalByFound(f.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Case", "for found report " + f.getId()));
            int score = MatchScorer.score(person(m), person(f)).map(MatchScorer.Result::score).orElse(0);
            // order matters for the DB unique keys: close/merge the found-side case FIRST
            foundCase.mergeInto(c);
            cases.saveAndFlush(foundCase);
            caseEvents.save(new CaseEvent(foundCase.getId(), CaseStatus.REPORTED, CaseStatus.CLOSED, "Merged into case " + c.getId(), me));
            c.attachFound(f, score);
            m.setStatus(ReportStatus.MATCHED);
            f.setStatus(ReportStatus.MATCHED);
        } else {
            if (c.getFoundReport() == null || c.getMember() == null) {
                throw new BusinessRuleException("FOUND_REPORT_REQUIRED", "foundReportId is required for this case");
            }
            if (c.getMissingReport() == null) {
                missing.findActiveForMember(c.getMember().getId(), ACTIVE).ifPresent(mr ->
                        cases.findCanonicalByMissing(mr.getId()).ifPresent(missingCase -> {
                            missingCase.mergeInto(c);
                            cases.saveAndFlush(missingCase);
                            caseEvents.save(new CaseEvent(missingCase.getId(), CaseStatus.REPORTED, CaseStatus.CLOSED, "Merged into case " + c.getId(), me));
                            c.attachMissing(mr);
                            mr.setStatus(ReportStatus.MATCHED);
                        }));
            }
            c.getFoundReport().setStatus(ReportStatus.MATCHED);
        }
        changeStatus(c, CaseStatus.MATCHED, req.note() == null ? "Match confirmed by desk" : req.note(), desk);
        cases.saveAndFlush(c);
        notifications.notify(c.guardian(), c.getId(), NotificationType.MATCH_CONFIRMED,
                "A match was confirmed. Please go to " + (c.getAssignedDesk() == null ? "the help desk" : c.getAssignedDesk().getName())
                        + " with your one-time code.");
        otp.issueFor(c, desk);
        return mapper.toResponse(c, desk);
    }

    /** Undo a wrong match: case back to REPORTED, the merged-in case is reopened, pending OTP cancelled. */
    @Transactional
    public CaseResponse rejectMatch(Long caseId, String note, AppUserPrincipal desk) {
        CaseFile c = loadCanonical(caseId);
        access.assertEventRole(desk, c.getEventId(), Role.DESK);
        if (c.getStatus() != CaseStatus.MATCHED) throw new InvalidStateException("Only a MATCHED case can be rejected");
        otp.cancelActive(c.getId());
        User me = users.getReferenceById(desk.id());
        List<CaseFile> merged = cases.findMergedInto(c.getId());

        // status first, then detach in the SAME flush (a MATCHED case without a pair would break ck_cf_pair)
        CaseStatus from = c.moveTo(CaseStatus.REPORTED);
        for (CaseFile other : merged) {
            if (other.getFoundReport() != null && other.getFoundReport().equals(c.getFoundReport())) {
                c.detachFound().setStatus(ReportStatus.OPEN);
            } else if (other.getMissingReport() != null && other.getMissingReport().equals(c.getMissingReport())) {
                c.detachMissing().setStatus(ReportStatus.OPEN);
            }
        }
        if (c.getMissingReport() != null) c.getMissingReport().setStatus(ReportStatus.OPEN);
        if (c.getFoundReport() != null) c.getFoundReport().setStatus(ReportStatus.OPEN);
        cases.saveAndFlush(c);
        for (CaseFile other : merged) {          // now the reports are free again: reopen the other case(s)
            other.reopenAfterUnmerge();
            cases.saveAndFlush(other);
            caseEvents.save(new CaseEvent(other.getId(), CaseStatus.CLOSED, CaseStatus.REPORTED, "Reopened: match with case " + c.getId() + " rejected", me));
        }
        caseEvents.save(new CaseEvent(c.getId(), from, CaseStatus.REPORTED, note == null ? "Match rejected by desk" : note, me));
        notifications.notify(c.guardian(), c.getId(), NotificationType.STATUS_CHANGED, "The suggested match was not confirmed. The search continues.");
        audit.log(desk.id(), "MATCH_REJECTED", "CASE", c.getId(), null);
        return mapper.toResponse(c, desk);
    }

    @Transactional
    public CaseResponse arrived(Long caseId, AppUserPrincipal desk) {
        CaseFile c = loadCanonical(caseId);
        access.assertEventRole(desk, c.getEventId(), Role.DESK);
        changeStatus(c, CaseStatus.AT_DESK, "Person and guardian at the desk", desk);
        return mapper.toResponse(c, desk);
    }

    @Transactional
    public CaseResponse assign(Long caseId, AssignRequest req, AppUserPrincipal police) {
        CaseFile c = loadCanonical(caseId);
        access.assertEventRole(police, c.getEventId(), Role.POLICE);
        HelpDesk desk = null;
        User officer = null;
        if (req.deskId() != null) {
            desk = desks.findWithEvent(req.deskId()).orElseThrow(() -> new ResourceNotFoundException("Desk", req.deskId()));
            if (!desk.getEventId().equals(c.getEventId())) throw new BusinessRuleException("DESK_OTHER_EVENT", "Desk belongs to another event");
        }
        if (req.officerId() != null) {
            officer = users.findById(req.officerId()).orElseThrow(() -> new ResourceNotFoundException("User", req.officerId()));
            if (officer.getRole() != Role.POLICE) throw new BusinessRuleException("NOT_POLICE", "Officer must be a POLICE user");
        }
        c.assign(desk, officer);
        cases.saveAndFlush(c);
        caseEvents.save(new CaseEvent(c.getId(), c.getStatus(), c.getStatus(), "Assigned"
                + (desk != null ? " to " + desk.getName() : "") + (officer != null ? " / officer " + officer.getFullName() : ""),
                users.getReferenceById(police.id())));
        audit.log(police.id(), "CASE_ASSIGNED", "CASE", caseId, null);
        return mapper.toResponse(c, police);
    }

    /** Police (or admin) close a case; a family may withdraw its own REPORTED missing-person case. */
    @Transactional
    public CaseResponse close(Long caseId, String note, AppUserPrincipal user) {
        CaseFile c = loadCanonical(caseId);
        if (user.role() == Role.FAMILY) {
            access.assertCanViewCase(user, c);
            boolean ownReport = c.getMissingReport() != null && c.getMissingReport().getReportedBy().getId().equals(user.id());
            if (!ownReport || c.getStatus() != CaseStatus.REPORTED) {
                throw new AccessDeniedException("Families can only withdraw their own unmatched missing report");
            }
        } else {
            access.assertEventRole(user, c.getEventId(), Role.POLICE);
        }
        otp.cancelActive(c.getId());
        if (c.getMissingReport() != null && c.getMissingReport().getStatus() != ReportStatus.RESOLVED) c.getMissingReport().setStatus(ReportStatus.CLOSED);
        if (c.getFoundReport() != null && c.getFoundReport().getStatus() != ReportStatus.RESOLVED) c.getFoundReport().setStatus(ReportStatus.CLOSED);
        changeStatus(c, CaseStatus.CLOSED, note, user);
        return mapper.toResponse(c, user);
    }

    // ---- helpers -------------------------------------------------------------------

    private void changeStatus(CaseFile c, CaseStatus next, String note, AppUserPrincipal actor) {
        CaseStatus from = c.moveTo(next);
        cases.saveAndFlush(c);
        caseEvents.save(new CaseEvent(c.getId(), from, next, note, users.getReferenceById(actor.id())));
        if (next != CaseStatus.MATCHED) {
            notifications.notify(c.guardian(), c.getId(), NotificationType.STATUS_CHANGED, "Case " + c.getId() + " is now " + next + ".");
        }
        audit.log(actor.id(), "CASE_" + next, "CASE", c.getId(), from + "->" + next);
    }

    private CaseFile loadCanonical(Long id) {
        CaseFile c = cases.findWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Case", id));
        if (!c.isCanonical()) {
            throw new InvalidStateException("Case " + id + " was merged into case " + c.getMergedInto().getId());
        }
        return c;
    }

    private static MatchScorer.Person person(MissingReport m) {
        return new MatchScorer.Person(m.getGender(), m.getApproxAge(), m.getClothingColor(), m.getLastSeenLat().doubleValue(),
                m.getLastSeenLng().doubleValue(), m.getLastSeenAt());
    }

    private static MatchScorer.Person person(FoundReport f) {
        return new MatchScorer.Person(f.getGender(), f.getApproxAge(), f.getClothingColor(), f.getFoundLat().doubleValue(),
                f.getFoundLng().doubleValue(), f.getFoundAt());
    }

    private static String roleLabel(Role r) {
        return switch (r) {
            case DESK -> "Help desk";
            case VOLUNTEER -> "Volunteer";
            case POLICE -> "Police";
            case ADMIN -> "Organiser";
            case FAMILY -> "You";
        };
    }

    static LocalDateTime startOfToday() { return LocalDate.now().atStartOfDay(); }
}
