package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.FoundReportRequest;
import com.rajesh.milansetu.dto.request.MissingReportRequest;
import com.rajesh.milansetu.dto.response.CaseResponse;
import com.rajesh.milansetu.dto.response.FoundReportResponse;
import com.rajesh.milansetu.dto.response.MissingReportResponse;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.repository.*;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;

@Service
public class ReportService {
    private static final EnumSet<ReportStatus> ACTIVE = EnumSet.of(ReportStatus.OPEN, ReportStatus.MATCHED);

    private final MissingReportRepository missing;
    private final FoundReportRepository found;
    private final MemberRepository members;
    private final HelpDeskRepository desks;
    private final CaseFileRepository cases;
    private final CaseEventRepository caseEvents;
    private final EventRepository eventRepo;
    private final UserRepository users;
    private final EventService events;
    private final CaseMapper mapper;
    private final AccessPolicy access;
    private final AuditService audit;

    public ReportService(MissingReportRepository missing, FoundReportRepository found, MemberRepository members,
                         HelpDeskRepository desks, CaseFileRepository cases, CaseEventRepository caseEvents,
                         EventRepository eventRepo, UserRepository users, EventService events, CaseMapper mapper,
                         AccessPolicy access, AuditService audit) {
        this.missing = missing;
        this.found = found;
        this.members = members;
        this.desks = desks;
        this.cases = cases;
        this.caseEvents = caseEvents;
        this.eventRepo = eventRepo;
        this.users = users;
        this.events = events;
        this.mapper = mapper;
        this.access = access;
        this.audit = audit;
    }

    /** A family reports a missing person (registered member or not). Opens a REPORTED case. */
    @Transactional
    public CaseResponse reportMissing(MissingReportRequest req, AppUserPrincipal family) {
        Event event = events.requireActive(req.eventId());
        Member member = null;
        String name = req.personName() == null ? null : req.personName().trim();
        if (req.memberId() != null) {
            member = members.findWithDetails(req.memberId()).orElseThrow(() -> new ResourceNotFoundException("Member", req.memberId()));
            access.assertOwnsMember(family, member);
            if (!member.getEventId().equals(event.getId())) {
                throw new BusinessRuleException("MEMBER_OTHER_EVENT", "This member is registered for a different event");
            }
            if (missing.findActiveForMember(member.getId(), ACTIVE).isPresent()) {
                throw new BusinessRuleException("REPORT_EXISTS", "There is already an open missing report for this member");
            }
            if (name == null || name.isBlank()) name = member.getFullName();
        }
        if (name == null || name.isBlank()) throw new BusinessRuleException("NAME_REQUIRED", "personName is required");
        if (req.lastSeenAt().isAfter(LocalDateTime.now())) throw new BusinessRuleException("FUTURE_TIME", "Last-seen time is in the future");

        MissingReport report = missing.save(new MissingReport(event, users.getReferenceById(family.id()), member, name,
                req.approxAge(), req.gender(), req.clothingColor(), req.lastSeenLat(), req.lastSeenLng(), req.lastSeenAt()));
        CaseFile c = cases.save(CaseFile.forMissing(report));
        caseEvents.save(new CaseEvent(c.getId(), null, CaseStatus.REPORTED, "Missing report filed by family", users.getReferenceById(family.id())));
        audit.log(family.id(), "MISSING_REPORTED", "CASE", c.getId(), null);
        return mapper.toResponse(c, family);
    }

    /** Volunteer or desk registers a found person who has no wristband. */
    @Transactional
    public CaseResponse reportFound(FoundReportRequest req, AppUserPrincipal staff) {
        Event event = events.requireActive(req.eventId());
        access.assertEventRole(staff, event.getId(), Role.VOLUNTEER, Role.DESK);
        HelpDesk desk;
        if (req.deskId() != null) {
            desk = desks.findWithEvent(req.deskId()).orElseThrow(() -> new ResourceNotFoundException("Desk", req.deskId()));
            if (!desk.getEventId().equals(event.getId())) throw new BusinessRuleException("DESK_OTHER_EVENT", "Desk belongs to another event");
        } else {
            desk = DeskLocator.nearest(desks.findByEvent_IdAndActiveTrue(event.getId()), req.foundLat().doubleValue(), req.foundLng().doubleValue())
                    .map(DeskLocator.Nearest::desk)
                    .orElseThrow(() -> new BusinessRuleException("NO_DESK", "This event has no active help desk"));
        }
        LocalDateTime when = req.foundAt() == null ? LocalDateTime.now() : req.foundAt();
        FoundReport report = found.save(new FoundReport(event, desk, users.getReferenceById(staff.id()), null, FoundSource.MANUAL,
                req.approxAge(), req.gender(), req.clothingColor(), req.foundLat(), req.foundLng(), when, req.safeNotes()));
        CaseFile c = cases.save(CaseFile.forFound(report));
        caseEvents.save(new CaseEvent(c.getId(), null, CaseStatus.REPORTED, "Found person registered (no wristband)", users.getReferenceById(staff.id())));
        audit.log(staff.id(), "FOUND_REPORTED", "CASE", c.getId(), "desk=" + desk.getId());
        return mapper.toResponse(c, staff);
    }

    @Transactional(readOnly = true)
    public Page<MissingReportResponse> listMissing(ReportStatus status, Pageable pageable, AppUserPrincipal user) {
        if (user.role() == Role.FAMILY) return missing.searchForReporter(user.id(), status, pageable).map(MissingReportResponse::from);
        return missing.searchForEvents(eventScope(user), status, pageable).map(MissingReportResponse::from);
    }

    @Transactional(readOnly = true)
    public Page<FoundReportResponse> listFound(ReportStatus status, Pageable pageable, AppUserPrincipal user) {
        return found.search(eventScope(user), status, pageable).map(FoundReportResponse::from);
    }

    private List<Long> eventScope(AppUserPrincipal user) {
        List<Long> ids = access.staffEvents(user);
        if (ids == null) ids = eventRepo.findAll().stream().map(Event::getId).toList();
        return ids.isEmpty() ? List.of(-1L) : ids;   // "in ()" is invalid SQL
    }
}
