package com.jbh.notification.contracts;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
    @NotNull(message = "Recipient ID is required")
    UUID recipientId,

    @NotBlank(message = "Recipient email is required")
    @Email(message = "Recipient email must be valid")
    String recipientEmail,

    UUID senderUserId,

    @Email(message = "Sender email must be valid")
    String senderEmail,

    String subject,

    String message,

    EmailTemplate emailTemplate,

    Map<String, Object> metadata
) {
}
