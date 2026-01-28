package com.jbh.notification.infra.email;

import java.util.List;
import lombok.Builder;

/**
 * Generic email request containing all information needed to send an email.
 * This record follows SRP - it only holds email data, not business logic.
 */
@Builder
@SuppressWarnings("PMD.UseObjectForClearerAPI")
public record EmailRequest(
    String to,
    String subject,
    String htmlBody,
    String textBody,
    List<String> cc,
    List<String> bcc
) {

    public static EmailRequest simple(final String to, final String subject, final String htmlBody) {
        return EmailRequest.builder()
            .to(to)
            .subject(subject)
            .htmlBody(htmlBody)
            .build();
    }

    public static EmailRequest withTextFallback(
            final String to,
            final String subject,
            final String htmlBody,
            final String textBody) {
        return EmailRequest.builder()
            .to(to)
            .subject(subject)
            .htmlBody(htmlBody)
            .textBody(textBody)
            .build();
    }
}
