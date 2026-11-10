package com.rajesh.milansetu.notification;

import com.rajesh.milansetu.entity.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * Local demo adapter: "delivers" messages into an in-memory outbox (last 200 messages).
 * The outbox is only readable through DevOutboxController, which exists only in the dev profile,
 * and only shows each user their own messages. Logs never contain the OTP.
 */
@Component
@ConditionalOnProperty(prefix = "app.notifications", name = "mode", havingValue = "mock", matchIfMissing = true)
public class MockNotificationSender implements NotificationSender {
    private static final Logger log = LoggerFactory.getLogger(MockNotificationSender.class);
    private static final int MAX = 200;

    public record OutboxMessage(Long userId, String subject, String body, LocalDateTime sentAt) { }

    private final Deque<OutboxMessage> outbox = new ArrayDeque<>();

    @Override
    public synchronized void send(User to, String subject, String body) {
        add(new OutboxMessage(to.getId(), subject, body, LocalDateTime.now()));
        log.info("[mock] message '{}' delivered to user {}", subject, to.getId());
    }

    @Override
    public synchronized void sendOtp(User guardian, Long caseId, String code, LocalDateTime expiresAt) {
        add(new OutboxMessage(guardian.getId(), "MilanSetu handover OTP (case " + caseId + ")",
                "Your one-time handover code is " + code + ". Show it ONLY at the help desk. Valid until " + expiresAt + ".",
                LocalDateTime.now()));
        log.info("[mock] OTP for case {} delivered to user {} (code not logged)", caseId, guardian.getId());
    }

    @Override
    public String channelName() { return "MOCK"; }

    public synchronized List<OutboxMessage> messagesFor(Long userId) {
        return outbox.stream().filter(m -> m.userId().equals(userId)).toList();
    }

    private void add(OutboxMessage m) {
        outbox.addFirst(m);
        while (outbox.size() > MAX) outbox.removeLast();
    }
}
