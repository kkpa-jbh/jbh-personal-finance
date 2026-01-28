package com.jbh.notification.infra.adapters.in.rest.vo;

import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.infra.persistence.NotificationStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
    UUID id,
    UUID recipientId,
    String recipientEmail,
    UUID senderUserId,
    String senderEmail,
    String subject,
    String message,
    NotificationType notificationType,
    NotificationStatus status,
    Boolean read,
    Map<String, Object> metadata,
    LocalDateTime sentAt,
    LocalDateTime createdAt
) {
}
