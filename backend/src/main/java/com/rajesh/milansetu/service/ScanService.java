package com.rajesh.milansetu.service;

import com.rajesh.milansetu.dto.request.ScanRequest;
import com.rajesh.milansetu.dto.response.ScanResponse;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.notification.NotificationService;
import com.rajesh.milansetu.repository.*;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

/**
 * QR scan workflow. Invalid, revoked or inactive-event tokens all give the SAME generic 404,
 * so a scanner cannot learn anything by trying tokens.
 */
@Service
public class ScanService {
    static final String GENERIC_INVALID = "Wristband not recognised. Take the person to the nearest help desk.";
    private static final EnumSet<ReportStatus> ACTIVE = EnumSet.of(ReportStatus.OPEN, ReportStatus.MATCHED);

    private final MemberRepository members;
    private final FoundReportRepository found;
    private final MissingReportRepository missing;
    private final CaseFileRepository cases;
    private final CaseEventRepository caseEvents;
    private final HelpDeskRepository desks;
    private final UserRepository users;
    private final NotificationService notifications;
    private final AccessPolicy access;
    private final AuditService audit;

    public ScanService(MemberRepository members, FoundReportRepository found, MissingReportRepository missing,
                       CaseFileRepository cases, CaseEventRepository caseEvents, HelpDeskRepository desks,
                       UserRepository users, NotificationService notifications, AccessPolicy access, AuditService audit) {
        this.members = members;
        this.found = found;
        this.missing = missing;
        this.cases = cases;
        this.caseEvents = caseEvents;
        this.desks = desks;
        this.users = users;
        this.notifications = notifications;
        this.access = access;
        this.audit = audit;
    }

    @Transactional
    public ScanResponse scan(String rawToken, ScanRequest req, AppUserPrincipal scanner) {
        String token = QrTokens.parse(rawToken).orElseThrow(ScanService::invalid);
        Member member = members.findActiveByToken(token).orElseThrow(ScanService::invalid);
        Event event = member.getEvent();
        if (!event.isActive()) throw invalid();
        access.assertEventRole(scanner, event.getId(), Role.VOLUNTEER, Role.DESK);

        HelpDesk desk;
        Double distance = null;
        List<HelpDesk> eventDesks = desks.findByEvent_IdAndActiveTrue(event.getId());
        if (req != null && req.lat() != null && req.lng() != null) {
            DeskLocator.Nearest n = DeskLocator.nearest(eventDesks, req.lat().doubleValue(), req.lng().doubleValue())
                    .orElseThrow(() -> new BusinessRuleException("NO_DESK", "This event has no active help desk"));
            desk = n.desk();
            distance = GeoUtils.round2(n.distanceKm());
        } else {
            Long home = access.homeDesk(scanner, event.getId());
            desk = eventDesks.stream().filter(d -> d.getId().equals(home)).findFirst()
                    .or(() -> eventDesks.stream().findFirst())
                    .orElseThrow(() -> new BusinessRuleException("NO_DESK", "This event has no active help desk"));
        }

        // Second scan while a case is still open: return the existing case instead of creating a duplicate.
        Optional<FoundReport> existing = found.findActiveForMember(member.getId(), ACTIVE);
        if (existing.isPresent()) {
            CaseFile c = cases.findCanonicalByFound(existing.get().getId())
                    .or(() -> existing.flatMap(f -> missing.findActiveForMember(member.getId(), ACTIVE))
                            .flatMap(mr -> cases.findCanonicalByMissing(mr.getId())))
                    .orElseThrow(() -> new ResourceNotFoundException("Case", "for member"));
            audit.log(scanner.id(), "QR_RESCANNED", "CASE", c.getId(), null);
            return response(c.getId(), false, member, existing.get().getDesk(), distance);
        }

        BigDecimal lat = req != null && req.lat() != null ? req.lat() : desk.getLatitude();
        BigDecimal lng = req != null && req.lng() != null ? req.lng() : desk.getLongitude();
        ClothingColor colour = req != null && req.clothingColor() != null ? req.clothingColor() : ClothingColor.MULTI;
        FoundReport report = found.save(new FoundReport(event, desk, users.getReferenceById(scanner.id()), member,
                FoundSource.QR_SCAN, member.getAge(), member.getGender(), colour, lat, lng, LocalDateTime.now(), null));
        CaseFile c = cases.save(CaseFile.forFound(report));
        boolean familyReport = missing.findActiveForMember(member.getId(), ACTIVE).isPresent();
        caseEvents.save(new CaseEvent(c.getId(), null, CaseStatus.REPORTED,
                "Wristband scanned" + (familyReport ? "; family has an open missing report" : ""), users.getReferenceById(scanner.id())));
        notifications.notify(member.getGuardian(), c.getId(), NotificationType.MEMBER_FOUND,
                member.firstName() + " has been found and is being taken to " + desk.getName()
                        + ". Please go there. You will receive a one-time code for the handover.");
        audit.log(scanner.id(), "QR_SCANNED", "CASE", c.getId(), "desk=" + desk.getId());
        return response(c.getId(), true, member, desk, distance);
    }

    private static ScanResponse response(Long caseId, boolean created, Member m, HelpDesk desk, Double distance) {
        return new ScanResponse(caseId, created, m.firstName(), m.getAge(), m.getGender(),
                new ScanResponse.DeskInfo(desk.getId(), desk.getName(), desk.getLatitude(), desk.getLongitude(), distance),
                "Stay with " + m.firstName() + " and take them to " + desk.getName()
                        + ". The family has been notified. Do not hand the person to anyone outside the help desk.");
    }

    private static ResourceNotFoundException invalid() {
        return new ResourceNotFoundException(GENERIC_INVALID);
    }
}
