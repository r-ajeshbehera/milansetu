package com.rajesh.milansetu.notification;

import com.rajesh.milansetu.entity.User;

import java.time.LocalDateTime;

/** Strategy interface: Mock (local) or Email (Spring Mail). Chosen by app.notifications.mode. */
public interface NotificationSender {
    void send(User to, String subject, String body);

    /** The ONLY path an OTP ever travels. Implementations must never log the code. */
    void sendOtp(User guardian, Long caseId, String code, LocalDateTime expiresAt);

    String channelName();
}
