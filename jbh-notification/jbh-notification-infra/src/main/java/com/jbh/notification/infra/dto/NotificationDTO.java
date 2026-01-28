package com.jbh.notification.infra.dto;

import com.jbh.notification.contracts.EmailTemplate;
import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.infra.persistence.NotificationStatus;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Builder;

@Builder
public record NotificationDTO(
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
    EmailTemplate emailTemplate,
    Map<String, Object> metadata,
    LocalDateTime sentAt,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
