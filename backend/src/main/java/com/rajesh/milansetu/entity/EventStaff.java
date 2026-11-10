package com.rajesh.milansetu.entity;

import jakarta.persistence.*;

/** Which VOLUNTEER / DESK / POLICE user works at which event (event-level authorisation). */
@Entity
@Table(name = "event_staff")
public class EventStaff {
    @EmbeddedId
    private EventStaffId id;

    @Column(name = "desk_id")
    private Long deskId;

    @Column(name = "assigned_by", nullable = false)
    private Long assignedBy;

    protected EventStaff() { }

    public EventStaff(Long eventId, Long userId, Long deskId, Long assignedBy) {
        this.id = new EventStaffId(eventId, userId);
        this.deskId = deskId;
        this.assignedBy = assignedBy;
    }

    public EventStaffId getId() { return id; }
    public Long getDeskId() { return deskId; }
    public void setDeskId(Long deskId) { this.deskId = deskId; }
}
