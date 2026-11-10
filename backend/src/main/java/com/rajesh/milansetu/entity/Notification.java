package com.rajesh.milansetu.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** In-app inbox entry. Must NEVER contain an OTP or a phone number. */
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "case_id")
    private Long caseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 8)
    private NotificationChannel channel;

    @Column(nullable = false, length = 300)
    private String message;

    @Column(nullable = false)
    private boolean delivered;

    @Column(name = "read_at")
    private LocalDateTime readAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    protected Notification() { }

    public Notification(Long userId, Long caseId, NotificationType type, NotificationChannel channel, String message, boolean delivered) {
        this.userId = userId;
        this.caseId = caseId;
        this.type = type;
        this.channel = channel;
        this.message = message;
        this.delivered = delivered;
        this.createdAt = LocalDateTime.now();
    }

    public void markRead() { if (readAt == null) readAt = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public Long getCaseId() { return caseId; }
    public NotificationType getType() { return type; }
    public NotificationChannel getChannel() { return channel; }
    public String getMessage() { return message; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
