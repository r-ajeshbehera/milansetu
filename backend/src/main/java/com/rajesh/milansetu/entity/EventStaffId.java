package com.rajesh.milansetu.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class EventStaffId implements Serializable {
    @Column(name = "event_id")
    private Long eventId;

    @Column(name = "user_id")
    private Long userId;

    protected EventStaffId() { }

    public EventStaffId(Long eventId, Long userId) {
        this.eventId = eventId;
        this.userId = userId;
    }

    public Long getEventId() { return eventId; }
    public Long getUserId() { return userId; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof EventStaffId that)) return false;
        return Objects.equals(eventId, that.eventId) && Objects.equals(userId, that.userId);
    }

    @Override public int hashCode() { return Objects.hash(eventId, userId); }
}
