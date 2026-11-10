package com.rajesh.milansetu.dto.response;

import com.rajesh.milansetu.entity.Notification;
import com.rajesh.milansetu.entity.NotificationType;
import java.time.LocalDateTime;

public record NotificationResponse(Long id, Long caseId, NotificationType type, String message, boolean read,
                                   LocalDateTime createdAt) {
    public static NotificationResponse from(Notification n) {
        return new NotificationResponse(n.getId(), n.getCaseId(), n.getType(), n.getMessage(), n.getReadAt() != null,
                n.getCreatedAt());
    }
}
