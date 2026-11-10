package com.rajesh.milansetu.controller;

import com.rajesh.milansetu.notification.MockNotificationSender;
import com.rajesh.milansetu.security.CurrentUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * DEVELOPMENT ONLY ("dev" profile + mock notifications). Simulates the user's e-mail/SMS inbox so the OTP flow can
 * be demonstrated locally. Each user sees only messages addressed to them. This bean does not exist in production.
 */
@RestController
@RequestMapping("/api/dev/outbox")
@Profile("dev")
@ConditionalOnProperty(prefix = "app.notifications", name = "mode", havingValue = "mock", matchIfMissing = true)
@Tag(name = "Development: simulated inbox")
public class DevOutboxController {
    private final MockNotificationSender sender;

    public DevOutboxController(MockNotificationSender sender) {
        this.sender = sender;
    }

    @GetMapping
    @Operation(summary = "My simulated inbox (dev profile only)")
    public List<MockNotificationSender.OutboxMessage> mine() {
        return sender.messagesFor(CurrentUser.get().id());
    }
}
