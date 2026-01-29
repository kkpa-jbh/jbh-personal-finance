package com.jbh.notification.contracts;

import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.email.EmailTemplateMetadata;
import java.util.Map;
import java.util.UUID;

public record SendNotificationRequest(
    UUID recipientId,
    String recipientEmail,
    UUID senderUserId,
    String senderEmail,
    String subject,
    String message,
    EmailTemplate emailTemplate,
    Map<String, Object> metadata) {

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

    public Builder templateMetadata(final EmailTemplateMetadata templateMetadata) {
      this.emailTemplate = templateMetadata.getEmailTemplate();
      this.metadata = templateMetadata.toMap();
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
          metadata);
    }
  }
}
