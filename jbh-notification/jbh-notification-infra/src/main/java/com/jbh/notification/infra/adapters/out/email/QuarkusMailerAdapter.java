package com.jbh.notification.infra.adapters.out.email;

import com.jbh.notification.infra.email.EmailRequest;
import com.jbh.notification.infra.ports.output.EmailSenderPort;
import io.quarkus.mailer.Mail;
import io.quarkus.mailer.Mailer;
import io.quarkus.mailer.reactive.ReactiveMailer;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Quarkus Mailer adapter implementing EmailSenderPort.
 * Responsible only for the actual email sending via SMTP.
 */
@ApplicationScoped
public class QuarkusMailerAdapter implements EmailSenderPort {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusMailerAdapter.class);

    private final ReactiveMailer reactiveMailer;
    private final Mailer mailer;

    @Inject
    public QuarkusMailerAdapter(final ReactiveMailer reactiveMailer, final Mailer mailer) {
        this.reactiveMailer = reactiveMailer;
        this.mailer = mailer;
    }

    @Override
    public Uni<Void> sendAsync(final EmailRequest emailRequest) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Sending async email to: {} with subject: {}",
                emailRequest.to(), emailRequest.subject());
        }

        final Mail mail = buildMail(emailRequest);

        return reactiveMailer
            .send(mail)
            .onItem()
            .invoke(() -> logSuccessIfEnabled(emailRequest.to()))
            .onFailure()
            .invoke(error -> logErrorIfEnabled(emailRequest.to(), error));
    }

    @Override
    public void sendSync(final EmailRequest emailRequest) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Sending sync email to: {} with subject: {}",
                emailRequest.to(), emailRequest.subject());
        }

        final Mail mail = buildMail(emailRequest);

        try {
            mailer.send(mail);
            logSuccessIfEnabled(emailRequest.to());
        } catch (final IllegalStateException | IllegalArgumentException e) {
            logErrorIfEnabled(emailRequest.to(), e);
            throw e;
        }
    }

    private void logSuccessIfEnabled(final String recipient) {
        if (LOG.isInfoEnabled()) {
            LOG.info("Email sent successfully to: {}", recipient);
        }
    }

    private void logErrorIfEnabled(final String recipient, final Throwable error) {
        if (LOG.isErrorEnabled()) {
            LOG.error("Failed to send email to: {}. Error: {}", recipient, error.getMessage());
        }
    }

    private Mail buildMail(final EmailRequest emailRequest) {
        final Mail mail = createMailWithContent(emailRequest);
        addRecipients(mail, emailRequest);
        return mail;
    }

    private Mail createMailWithContent(final EmailRequest emailRequest) {
        if (hasHtmlBody(emailRequest)) {
            final Mail mail = Mail.withHtml(
                emailRequest.to(),
                emailRequest.subject(),
                emailRequest.htmlBody()
            );
            if (hasTextBody(emailRequest)) {
                mail.setText(emailRequest.textBody());
            }
            return mail;
        }
        return Mail.withText(
            emailRequest.to(),
            emailRequest.subject(),
            emailRequest.textBody() != null ? emailRequest.textBody() : ""
        );
    }

    private boolean hasHtmlBody(final EmailRequest emailRequest) {
        return emailRequest.htmlBody() != null && !emailRequest.htmlBody().isEmpty();
    }

    private boolean hasTextBody(final EmailRequest emailRequest) {
        return emailRequest.textBody() != null && !emailRequest.textBody().isEmpty();
    }

    private void addRecipients(final Mail mail, final EmailRequest emailRequest) {
        if (emailRequest.cc() != null && !emailRequest.cc().isEmpty()) {
            emailRequest.cc().forEach(mail::addCc);
        }
        if (emailRequest.bcc() != null && !emailRequest.bcc().isEmpty()) {
            emailRequest.bcc().forEach(mail::addBcc);
        }
    }
}
