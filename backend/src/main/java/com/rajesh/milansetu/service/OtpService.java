package com.rajesh.milansetu.service;

import com.rajesh.milansetu.config.AppProperties;
import com.rajesh.milansetu.dto.response.CaseResponse;
import com.rajesh.milansetu.dto.response.OtpIssuedResponse;
import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.exception.BusinessRuleException;
import com.rajesh.milansetu.exception.InvalidStateException;
import com.rajesh.milansetu.exception.OtpRejectedException;
import com.rajesh.milansetu.exception.ResourceNotFoundException;
import com.rajesh.milansetu.notification.NotificationService;
import com.rajesh.milansetu.repository.*;
import com.rajesh.milansetu.security.AccessPolicy;
import com.rajesh.milansetu.security.AppUserPrincipal;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * Secure handover:
 *  issue  -> SecureRandom 6 digits, BCrypt hash stored, 10-minute expiry, old ACTIVE code superseded,
 *            plain code sent ONLY to the guardian through NotificationSender (never returned, never logged).
 *  verify -> case must be AT_DESK; 5 wrong attempts lock the code; correct code marks it USED and the case REUNITED.
 *            @Version on case + OTP: two desks verifying at once -> one wins, the other gets 409.
 */
@Service
public class OtpService {
    private final HandoverOtpRepository otps;
    private final CaseFileRepository cases;
    private final CaseEventRepository caseEvents;
    private final UserRepository users;
    private final NotificationService notifications;
    private final PasswordEncoder encoder;
    private final AccessPolicy access;
    private final CaseMapper mapper;
    private final AuditService audit;
    private final AppProperties.Otp cfg;

    public OtpService(HandoverOtpRepository otps, CaseFileRepository cases, CaseEventRepository caseEvents,
                      UserRepository users, NotificationService notifications, PasswordEncoder encoder,
                      AccessPolicy access, CaseMapper mapper, AuditService audit, AppProperties props) {
        this.otps = otps;
        this.cases = cases;
        this.caseEvents = caseEvents;
        this.users = users;
        this.notifications = notifications;
        this.encoder = encoder;
        this.access = access;
        this.mapper = mapper;
        this.audit = audit;
        this.cfg = props.otp();
    }

    @Transactional
    public OtpIssuedResponse issue(Long caseId, AppUserPrincipal desk) {
        CaseFile c = load(caseId);
        access.assertEventRole(desk, c.getEventId(), Role.DESK);
        return issueFor(c, desk);
    }

    /** Also called by CaseService.confirmMatch (same transaction). */
    @Transactional
    public OtpIssuedResponse issueFor(CaseFile c, AppUserPrincipal desk) {
        if (!c.isCanonical() || (c.getStatus() != CaseStatus.MATCHED && c.getStatus() != CaseStatus.AT_DESK)) {
            throw new InvalidStateException("An OTP can only be issued for a MATCHED or AT_DESK case");
        }
        User guardian = c.guardian();
        if (guardian == null) throw new BusinessRuleException("NO_GUARDIAN", "No guardian is linked to this case");
        LocalDateTime now = LocalDateTime.now();
        if (otps.countByCaseIdAndCreatedAtAfter(c.getId(), now.minusHours(1)) >= cfg.maxIssuesPerHour()) {
            throw new BusinessRuleException("OTP_LIMIT", "Too many OTPs issued for this case in the last hour");
        }
        otps.findByCaseIdAndStatus(c.getId(), OtpStatus.ACTIVE).ifPresent(old -> {
            old.supersede();
            otps.saveAndFlush(old);            // must hit the DB before the new ACTIVE row (unique key)
        });
        String code = OtpGenerator.generate();
        HandoverOtp otp = otps.save(new HandoverOtp(c.getId(), encoder.encode(code), now.plusMinutes(cfg.expiryMinutes()), desk.id()));
        notifications.sendOtp(guardian, c.getId(), code, otp.getExpiresAt());
        audit.log(desk.id(), "OTP_ISSUED", "CASE", c.getId(), null);
        return new OtpIssuedResponse(c.getId(), otp.getExpiresAt(), "guardian via " + notifications.channel().toLowerCase());
    }

    /** noRollbackFor: a wrong code must still save attempts+1 / LOCKED / EXPIRED. */
    @Transactional(noRollbackFor = OtpRejectedException.class)
    public CaseResponse verifyAndHandover(Long caseId, String code, AppUserPrincipal desk) {
        CaseFile c = load(caseId);
        access.assertEventRole(desk, c.getEventId(), Role.DESK);
        if (c.getStatus() != CaseStatus.AT_DESK) {
            throw new InvalidStateException("Handover is only possible when the case is AT_DESK (current: " + c.getStatus() + ")");
        }
        HandoverOtp otp = otps.findFirstByCaseIdOrderByCreatedAtDesc(c.getId())
                .orElseThrow(() -> new OtpRejectedException(HttpStatus.CONFLICT, "OTP_NOT_ISSUED", "No OTP has been issued for this case", 0));

        HandoverOtp.Result result = otp.verify(code, LocalDateTime.now(), raw -> encoder.matches(raw, otp.getOtpHash()), cfg.maxAttempts());
        otps.saveAndFlush(otp);

        switch (result) {
            case SUCCESS -> {
                CaseStatus from = c.getStatus();
                c.markReunited();
                if (c.getMissingReport() != null) c.getMissingReport().setStatus(ReportStatus.RESOLVED);
                if (c.getFoundReport() != null) c.getFoundReport().setStatus(ReportStatus.RESOLVED);
                cases.saveAndFlush(c);          // optimistic lock: a second desk fails here
                caseEvents.save(new CaseEvent(c.getId(), from, CaseStatus.REUNITED, "Handover verified with OTP", users.getReferenceById(desk.id())));
                notifications.notify(c.guardian(), c.getId(), NotificationType.REUNITED, "Reunited. Thank you for using MilanSetu.");
                audit.log(desk.id(), "HANDOVER_COMPLETED", "CASE", c.getId(), null);
                return mapper.toResponse(c, desk);
            }
            case WRONG_CODE -> {
                audit.log(desk.id(), "OTP_FAILED", "CASE", c.getId(), "attempts=" + otp.getAttempts());
                throw new OtpRejectedException(HttpStatus.UNPROCESSABLE_ENTITY, "OTP_INVALID", "Incorrect OTP", otp.attemptsLeft(cfg.maxAttempts()));
            }
            case LOCKED_NOW -> {
                audit.log(desk.id(), "OTP_LOCKED", "CASE", c.getId(), null);
                throw new OtpRejectedException(HttpStatus.CONFLICT, "OTP_LOCKED", "Too many wrong attempts. Issue a new OTP.", 0);
            }
            case LOCKED -> throw new OtpRejectedException(HttpStatus.CONFLICT, "OTP_LOCKED", "This OTP is locked. Issue a new OTP.", 0);
            case EXPIRED -> throw new OtpRejectedException(HttpStatus.CONFLICT, "OTP_EXPIRED", "This OTP has expired. Issue a new OTP.", 0);
            case ALREADY_USED -> throw new OtpRejectedException(HttpStatus.CONFLICT, "OTP_USED", "This OTP was already used.", 0);
            default -> throw new OtpRejectedException(HttpStatus.CONFLICT, "OTP_NOT_ACTIVE", "This OTP is no longer valid. Issue a new OTP.", 0);
        }
    }

    /** Invalidate a pending code (used when a match is rejected or a case is closed). */
    @Transactional
    public void cancelActive(Long caseId) {
        otps.findByCaseIdAndStatus(caseId, OtpStatus.ACTIVE).ifPresent(HandoverOtp::supersede);
    }

    private CaseFile load(Long id) {
        CaseFile c = cases.findWithDetails(id).orElseThrow(() -> new ResourceNotFoundException("Case", id));
        if (!c.isCanonical()) throw new InvalidStateException("Case " + id + " was merged into case " + c.getMergedInto().getId());
        return c;
    }
}
