package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "missing_reports")
public class MissingReport {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "reported_by", nullable = false)
    private User reportedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @Column(name = "person_name", nullable = false, length = 120)
    private String personName;

    @Column(name = "approx_age", nullable = false)
    private int approxAge;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "clothing_color", nullable = false, length = 12)
    private ClothingColor clothingColor;

    @Column(name = "last_seen_lat", nullable = false, precision = 9, scale = 6)
    private BigDecimal lastSeenLat;

    @Column(name = "last_seen_lng", nullable = false, precision = 9, scale = 6)
    private BigDecimal lastSeenLng;

    @Column(name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private ReportStatus status = ReportStatus.OPEN;

    protected MissingReport() { }

    public MissingReport(Event event, User reportedBy, Member member, String personName, int approxAge, Gender gender,
                         ClothingColor clothingColor, BigDecimal lastSeenLat, BigDecimal lastSeenLng, LocalDateTime lastSeenAt) {
        this.event = event;
        this.reportedBy = reportedBy;
        this.member = member;
        this.personName = personName;
        this.approxAge = approxAge;
        this.gender = gender;
        this.clothingColor = clothingColor;
        this.lastSeenLat = lastSeenLat;
        this.lastSeenLng = lastSeenLng;
        this.lastSeenAt = lastSeenAt;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Long getEventId() { return event.getId(); }
    public User getReportedBy() { return reportedBy; }
    public Member getMember() { return member; }
    public String getPersonName() { return personName; }
    public int getApproxAge() { return approxAge; }
    public Gender getGender() { return gender; }
    public ClothingColor getClothingColor() { return clothingColor; }
    public BigDecimal getLastSeenLat() { return lastSeenLat; }
    public BigDecimal getLastSeenLng() { return lastSeenLng; }
    public LocalDateTime getLastSeenAt() { return lastSeenAt; }
    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }
    public void setIdForTest(Long id) { this.id = id; }
}
