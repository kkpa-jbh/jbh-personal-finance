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

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private UUID recipientId;
        private String recipientEmail;
        private UUID senderUserId;
        private String senderEmail;
        private String subject;
        private String message;
        private EmailTemplate emailTemplate;
        private Map<String, Object> metadata;

        private Builder() {}

        public Builder recipientId(final UUID recipientId) {
            this.recipientId = recipientId;
            return this;
        }

        public Builder recipientEmail(final String recipientEmail) {
            this.recipientEmail = recipientEmail;
            return this;
        }

        public Builder senderUserId(final UUID senderUserId) {
            this.senderUserId = senderUserId;
            return this;
        }

        public Builder senderEmail(final String senderEmail) {
            this.senderEmail = senderEmail;
            return this;
        }

        public Builder subject(final String subject) {
            this.subject = subject;
            return this;
        }

        public Builder message(final String message) {
            this.message = message;
            return this;
        }

        public Builder emailTemplate(final EmailTemplate emailTemplate) {
            this.emailTemplate = emailTemplate;
            return this;
        }

        public Builder metadata(final Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }

        public SendNotificationRequest build() {
            return new SendNotificationRequest(
                recipientId,
                recipientEmail,
                senderUserId,
                senderEmail,
                subject,
                message,
                emailTemplate,
                metadata
            );
        }
    }
}
