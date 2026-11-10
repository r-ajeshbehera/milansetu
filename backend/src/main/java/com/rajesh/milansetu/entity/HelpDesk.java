package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "help_desks")
public class HelpDesk {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "event_id", nullable = false)
    private Event event;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 9, scale = 6)
    private BigDecimal longitude;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "in_charge_id")
    private User inCharge;

    @Column(nullable = false)
    private boolean active = true;

    protected HelpDesk() { }

    public HelpDesk(Event event, String name, BigDecimal latitude, BigDecimal longitude) {
        this.event = event;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Long getId() { return id; }
    public Event getEvent() { return event; }
    public Long getEventId() { return event.getId(); }
    public String getName() { return name; }
    public BigDecimal getLatitude() { return latitude; }
    public BigDecimal getLongitude() { return longitude; }
    public User getInCharge() { return inCharge; }
    public boolean isActive() { return active; }

    public void setName(String name) { this.name = name; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public void setInCharge(User inCharge) { this.inCharge = inCharge; }
    public void setActive(boolean active) { this.active = active; }
    public void setIdForTest(Long id) { this.id = id; }
}
