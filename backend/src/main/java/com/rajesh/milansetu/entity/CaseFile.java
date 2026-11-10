package com.rajesh.milansetu.entity;

import com.rajesh.milansetu.exception.InvalidStateException;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * One case per person-incident. Opened from a single report; a confirmed match merges the found-side case into the
 * missing-side case (merged_into_id). @Version stops two desks from completing the same handover.
 */
@Entity
@Table(name = "case_files")
public class CaseFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "missing_report_id")
    private MissingReport missingReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "found_report_id")
    private FoundReport foundReport;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "match_score")
    private Integer matchScore;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CaseStatus status = CaseStatus.REPORTED;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_desk_id")
    private HelpDesk assignedDesk;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_officer_id")
    private User assignedOfficer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merged_into_id")
    private CaseFile mergedInto;

    @Column(name = "opened_at", nullable = false)
    private LocalDateTime openedAt;

    @Column(name = "matched_at")
    private LocalDateTime matchedAt;

    @Column(name = "reunited_at")
    private LocalDateTime reunitedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "stale_flagged_at")
    private LocalDateTime staleFlaggedAt;

    @Version
    private Long version;

    protected CaseFile() { }

    public static CaseFile forMissing(MissingReport report) {
        CaseFile c = new CaseFile();
        c.event = report.getEvent();
        c.missingReport = report;
        c.member = report.getMember();
        c.openedAt = LocalDateTime.now();
        return c;
    }

    public static CaseFile forFound(FoundReport report) {
        CaseFile c = new CaseFile();
        c.event = report.getEvent();
        c.foundReport = report;
        c.member = report.getMember();
        c.assignedDesk = report.getDesk();
        c.openedAt = LocalDateTime.now();
        return c;
    }

    /** Generic, validated status change. REUNITED is impossible here (see markReunited). */
    public CaseStatus moveTo(CaseStatus next) {
        if (!status.canMoveTo(next)) {
            throw new InvalidStateException("Case " + id + " cannot move from " + status + " to " + next);
        }
        CaseStatus from = status;
        status = next;
        staleFlaggedAt = null;
        LocalDateTime now = LocalDateTime.now();
        if (next == CaseStatus.MATCHED) matchedAt = now;
        if (next == CaseStatus.REPORTED) matchedAt = null;
        if (next == CaseStatus.CLOSED) closedAt = now;
        return from;
    }

    /** Package-level guard: only called by OtpService after a successful OTP verification. */
    public void markReunited() {
        if (status != CaseStatus.AT_DESK) {
            throw new InvalidStateException("Only a case at the desk can be reunited (current: " + status + ")");
        }
        status = CaseStatus.REUNITED;
        reunitedAt = LocalDateTime.now();
        staleFlaggedAt = null;
    }

    /** Close this (found-side) case because it was merged into the missing-side case. */
    public void mergeInto(CaseFile target) {
        if (mergedInto != null) throw new InvalidStateException("Case " + id + " is already merged");
        this.mergedInto = target;
        this.status = CaseStatus.CLOSED;
        this.closedAt = LocalDateTime.now();
    }

    public void attachFound(FoundReport found, Integer score) {
        if (foundReport != null) throw new InvalidStateException("Case " + id + " already has a found report");
        this.foundReport = found;
        this.matchScore = score;
        if (member == null) member = found.getMember();
        if (assignedDesk == null) assignedDesk = found.getDesk();
    }

    /** Undo of attachFound (used when a wrong match is rejected). Call only after moving back to REPORTED. */
    public FoundReport detachFound() {
        FoundReport f = foundReport;
        foundReport = null;
        matchScore = null;
        return f;
    }

    public MissingReport detachMissing() {
        MissingReport m = missingReport;
        missingReport = null;
        return m;
    }

    /** A merged case becomes a normal open case again (wrong match undone). */
    public void reopenAfterUnmerge() {
        this.mergedInto = null;
        this.status = CaseStatus.REPORTED;
        this.closedAt = null;
    }

    public void attachMissing(MissingReport missing) {
        if (missingReport != null) throw new InvalidStateException("Case " + id + " already has a missing report");
        this.missingReport = missing;
    }

    /** Guardian = owner of the registered member, otherwise the person who filed the missing report. */
    public User guardian() {
        if (member != null) return member.getGuardian();
        if (missingReport != null) return missingReport.getReportedBy();
        return null;
    }

    public boolean isCanonical() { return mergedInto == null; }

    public void assign(HelpDesk desk, User officer) {
        if (desk != null) this.assignedDesk = desk;
        if (officer != null) this.assignedOfficer = officer;
    }

    public void flagStale() { this.staleFlaggedAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Long getEventId() { return event.getId(); }
    public MissingReport getMissingReport() { return missingReport; }
    public FoundReport getFoundReport() { return foundReport; }
    public Member getMember() { return member; }
    public Integer getMatchScore() { return matchScore; }
    public CaseStatus getStatus() { return status; }
    public HelpDesk getAssignedDesk() { return assignedDesk; }
    public User getAssignedOfficer() { return assignedOfficer; }
    public CaseFile getMergedInto() { return mergedInto; }
    public LocalDateTime getOpenedAt() { return openedAt; }
    public LocalDateTime getMatchedAt() { return matchedAt; }
    public LocalDateTime getReunitedAt() { return reunitedAt; }
    public LocalDateTime getClosedAt() { return closedAt; }
    public LocalDateTime getStaleFlaggedAt() { return staleFlaggedAt; }
    public void setIdForTest(Long id) { this.id = id; }
    public void setStatusForTest(CaseStatus status) { this.status = status; }
}
