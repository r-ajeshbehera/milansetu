package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "found_reports")
public class FoundReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "desk_id", nullable = false)
    private HelpDesk desk;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private FoundSource source;

    @Column(name = "approx_age", nullable = false)
    private int approxAge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "clothing_color", nullable = false, length = 12)
    private ClothingColor clothingColor;

    @Column(name = "found_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal foundLat;

    @Column(name = "found_lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal foundLng;

    @Column(name = "found_at", nullable = false)
    private LocalDateTime foundAt;

    @Column(name = "safe_notes", length = 500)
    private String safeNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.OPEN;

    protected FoundReport() { }

    public FoundReport(Event event, HelpDesk desk, User reportedBy, Member member, FoundSource source, int approxAge,
                       Gender gender, ClothingColor clothingColor, BigDecimal foundLat, BigDecimal foundLng,
                       LocalDateTime foundAt, String safeNotes) {
        this.event = event;
        this.desk = desk;
        this.reportedBy = reportedBy;
        this.member = member;
        this.source = source;
        this.approxAge = approxAge;
        this.gender = gender;
        this.clothingColor = clothingColor;
        this.foundLat = foundLat;
        this.foundLng = foundLng;
        this.foundAt = foundAt;
        this.safeNotes = safeNotes;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Long getEventId() { return event.getId(); }
    public HelpDesk getDesk() { return desk; }
    public User getReportedBy() { return reportedBy; }
    public Member getMember() { return member; }
    public FoundSource getSource() { return source; }
    public int getApproxAge() { return approxAge; }
    public Gender getGender() { return gender; }
    public ClothingColor getClothingColor() { return clothingColor; }
    public BigDecimal getFoundLat() { return foundLat; }
    public BigDecimal getFoundLng() { return foundLng; }
    public LocalDateTime getFoundAt() { return foundAt; }
    public String getSafeNotes() { return safeNotes; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setIdForTest(Long id) { this.id = id; }
}
