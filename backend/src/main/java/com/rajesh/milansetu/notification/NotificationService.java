package com.rajesh.milansetu.notification;

import com.rajesh.milansetu.entity.*;
import com.rajesh.milansetu.repository.NotificationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Writes the in-app notification and hands delivery to the configured sender.
 * Delivery failures are logged and never break the business transaction (a reunion must not fail because SMTP is down).
 */
@Service
public class NotificationService {
    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);
    private final NotificationRepository notifications;
    private final NotificationSender sender;

    public NotificationService(NotificationRepository notifications, NotificationSender sender) {
        this.notifications = notifications;
        this.sender = sender;
    }

    public void notify(User to, Long caseId, NotificationType type, String message) {
        if (to == null) return;
        boolean delivered = true;
        try {
            sender.send(to, "MilanSetu update", message);
        } catch (RuntimeException e) {
            delivered = false;
            log.warn("Notification delivery failed for user {}: {}", to.getId(), e.getClass().getSimpleName());
        }
        notifications.save(new Notification(to.getId(), caseId, type, NotificationChannel.IN_APP, message, delivered));
    }

    /** The code goes ONLY to the sender; the stored in-app message never contains it. */
    public void sendOtp(User guardian, Long caseId, String code, LocalDateTime expiresAt) {
        boolean delivered = true;
        try {
            sender.sendOtp(guardian, caseId, code, expiresAt);
        } catch (RuntimeException e) {
            delivered = false;
            log.warn("OTP delivery failed for case {}: {}", caseId, e.getClass().getSimpleName());
        }
        notifications.save(new Notification(guardian.getId(), caseId, NotificationType.OTP_SENT, NotificationChannel.IN_APP,
                "A one-time handover code was sent to you (" + sender.channelName().toLowerCase()
                        + "). Bring it to the help desk. It expires in 10 minutes.", delivered));
    }

    public String channel() { return sender.channelName(); }
}
