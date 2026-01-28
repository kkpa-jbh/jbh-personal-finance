package com.jbh.notification.infra.ports.output;

import com.jbh.notification.infra.email.EmailRequest;
import io.smallrye.mutiny.Uni;

/**
 * Output port for sending emails.
 * Follows SRP: responsible only for email sending operations (sync/async).
 */
public interface EmailSenderPort {

    /**
     * Sends an email asynchronously.
     *
     * @param emailRequest the email request containing all necessary information
     * @return Uni that completes when email is sent or fails
     */
    Uni<Void> sendAsync(EmailRequest emailRequest);

    /**
     * Sends an email synchronously (blocking).
     *
     * @param emailRequest the email request containing all necessary information
     */
    void sendSync(EmailRequest emailRequest);
}
