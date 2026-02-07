package com.jbh.notification.infra.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Logs mailer configuration at startup for debugging purposes. */
@SuppressWarnings("HARD_CODE_PASSWORD")
@ApplicationScoped
public class MailerConfigLogger {

  private static final Logger LOG = LoggerFactory.getLogger(MailerConfigLogger.class);
  private static final String NOT_SET = "NOT_SET";
  private static final String TRUE_VALUE = "true";
  private static final int MIN_EMAIL_PREFIX_LENGTH = 2;
  private static final int MIN_PASSWORD_VISIBLE_LENGTH = 4;

  @ConfigProperty(name = "quarkus.mailer.host", defaultValue = NOT_SET)
  String host;

  @ConfigProperty(name = "quarkus.mailer.port", defaultValue = NOT_SET)
  String port;

  @ConfigProperty(name = "quarkus.mailer.username", defaultValue = NOT_SET)
  String username;

  @ConfigProperty(name = "quarkus.mailer.password", defaultValue = NOT_SET)
  String password;

  @ConfigProperty(name = "quarkus.mailer.from", defaultValue = NOT_SET)
  String from;

  @ConfigProperty(name = "quarkus.mailer.tls", defaultValue = NOT_SET)
  String tls;

  @ConfigProperty(name = "quarkus.mailer.mock", defaultValue = NOT_SET)
  String mock;

  void onStart(@Observes final StartupEvent event) {
    logMailerConfiguration();
    logCredentialWarnings();
    logMailerMode();
  }

  private void logMailerConfiguration() {
    if (LOG.isInfoEnabled()) {
      LOG.info("========================================");
      LOG.info("MAILER CONFIGURATION DEBUG");
      LOG.info("========================================");
      LOG.info("quarkus.mailer.mock     = {}", mock);
      LOG.info("quarkus.mailer.host     = {}", host);
      LOG.info("quarkus.mailer.port     = {}", port);
      LOG.info("quarkus.mailer.from     = {}", from);
      LOG.info("quarkus.mailer.tls      = {}", tls);
      LOG.info("quarkus.mailer.username = {}", maskEmail(username));
      LOG.info("quarkus.mailer.password = {}", maskPassword(password));
      LOG.info("========================================");
    }
  }

  private void logCredentialWarnings() {
    if (LOG.isWarnEnabled() && (NOT_SET.equals(username) || NOT_SET.equals(password))) {
      LOG.warn(
          "GMAIL credentials not configured! Set GMAIL_USERNAME and GMAIL_APP_PASSWORD environment variables.");
    }
  }

  private void logMailerMode() {
    if (LOG.isInfoEnabled()) {
      if (TRUE_VALUE.equals(mock)) {
        LOG.info("Mailer is in MOCK mode - emails will be logged, not sent.");
      } else {
        LOG.info("Mailer is in REAL mode - emails will be sent via SMTP.");
      }
    }
  }

  private String maskEmail(final String email) {
    if (email == null || NOT_SET.equals(email) || email.isEmpty()) {
      return email;
    }
    final int atIndex = email.indexOf('@');
    if (atIndex <= MIN_EMAIL_PREFIX_LENGTH) {
      return "***" + email.substring(atIndex);
    }
    return email.substring(0, MIN_EMAIL_PREFIX_LENGTH) + "***" + email.substring(atIndex);
  }

  @SuppressWarnings("HARD_CODE_PASSWORD")
  private String maskPassword(final String pwd) {
    if (pwd == null || NOT_SET.equals(pwd) || pwd.isEmpty()) {
      return pwd;
    }
    final int length = pwd.length();
    return "****".concat(" (length: ").concat(String.valueOf(length)).concat(")");
  }
}
