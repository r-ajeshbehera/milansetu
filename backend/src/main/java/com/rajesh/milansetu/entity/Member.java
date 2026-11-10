package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.util.UUID;

/** A registered child or elderly person. The QR token is random and reveals nothing. */
@Entity
@Table(name = "members")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guardian_id", nullable = false)
    private User guardian;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(nullable = false)
    private int age;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Gender gender;

    /** PRIVATE family notes: never shown to volunteers or desk staff. */
    @Column(length = 500)
    private String notes;

    @Column(name = "qr_token", nullable = false, length = 36)
    private String qrToken;

    @Column(name = "qr_revoked", nullable = false)
    private boolean qrRevoked;

    protected Member() { }

    public Member(User guardian, Event event, String fullName, int age, Gender gender, String notes) {
        this.guardian = guardian;
        this.event = event;
        this.fullName = fullName;
        this.age = age;
        this.gender = gender;
        this.notes = notes;
        this.qrToken = UUID.randomUUID().toString();   // random v4 UUID: not guessable, not the DB id
    }

    /** A lost wristband can be replaced: the old token stops working immediately. */
    public void regenerateToken() {
        this.qrToken = UUID.randomUUID().toString();
        this.qrRevoked = false;
    }

    public String firstName() {
        String trimmed = fullName.trim();
        int space = trimmed.indexOf(' ');
        return space < 0 ? trimmed : trimmed.substring(0, space);
    }

    public Long getId() { return id; }
    public User getGuardian() { return guardian; }
    public Long getGuardianId() { return guardian.getId(); }
    public Event getEvent() { return event; }
    public Long getEventId() { return event.getId(); }
    public String getFullName() { return fullName; }
    public int getAge() { return age; }
    public Gender getGender() { return gender; }
    public String getNotes() { return notes; }
    public String getQrToken() { return qrToken; }
    public boolean isQrRevoked() { return qrRevoked; }
    public void setIdForTest(Long id) { this.id = id; }
}
