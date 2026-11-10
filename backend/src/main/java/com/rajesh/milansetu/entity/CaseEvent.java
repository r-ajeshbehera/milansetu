package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** One line of a case timeline. Written for EVERY status change. */
@Entity
@Table(name = "case_events")
public class CaseEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "case_id", nullable = false)
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 10)
    private CaseStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private CaseStatus status;

    @Column(length = 500)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    private User changedBy;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    protected CaseEvent() { }

    public CaseEvent(Long caseId, CaseStatus from, CaseStatus to, String note, User changedBy) {
        this.caseId = caseId;
        this.fromStatus = from;
        this.status = to;
        this.note = note;
        this.changedBy = changedBy;
        this.changedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Long getCaseId() { return caseId; }
    public CaseStatus getFromStatus() { return fromStatus; }
    public CaseStatus getStatus() { return status; }
    public String getNote() { return note; }
    public User getChangedBy() { return changedBy; }
    public LocalDateTime getChangedAt() { return changedAt; }
}
