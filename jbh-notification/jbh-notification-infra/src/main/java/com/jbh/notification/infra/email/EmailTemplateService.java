package com.jbh.notification.infra.email;

import com.jbh.notification.contracts.email.EmailTemplate;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import io.quarkus.qute.i18n.Localized;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Locale;
import java.util.Map;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for rendering email templates with internationalization support. */
@ApplicationScoped
public class EmailTemplateService {

  private static final Logger LOG = LoggerFactory.getLogger(EmailTemplateService.class);

  private static final String DEFAULT_LOCALE = "en";
  private static final String LOCALE_ES = "es";
  private static final String TEMPLATE_VAR_LOCALE = "locale";
  private static final String TEMPLATE_VAR_BASE_URL = "baseUrl";
  private static final String TEMPLATE_VAR_TEAM_NAME = "teamName";
  private static final String TEMPLATE_VAR_INVITER_NAME = "inviterName";
  private static final String TEMPLATE_VAR_ACCEPT_URL = "acceptUrl";
  private static final String TEMPLATE_VAR_MSG = "msg";
  private static final String ACCEPT_INVITATION_PATH = "/teams/%s/accept-invitation";
  private static final String DEFAULT_SUBJECT = "JBH Notification";

  @ConfigProperty(name = "jbh.frontend.url", defaultValue = "http://localhost:4200")
  String frontendUrl;

  @Inject
  @io.quarkus.qute.Location("emails/team-invitation.html")
  Template teamInvitationTemplate;

  @Inject
  @Localized("en")
  EmailMessages messagesEn;

  @Inject
  @Localized("es")
  EmailMessages messagesEs;

  /**
   * Renders a team invitation email.
   *
   * @param recipientEmail the recipient's email address
   * @param metadata the email metadata containing teamId, teamName, inviterName, locale
   * @return rendered EmailRequest with HTML and plain text content
   */
  public EmailRequest renderTeamInvitation(
      final String recipientEmail, final EmailMetadata metadata) {

    final String effectiveLocale = resolveLocale(metadata.getLocale());
    final EmailMessages messages = getMessages(effectiveLocale);
    final String acceptUrl = buildAcceptUrl(metadata.getTeamId());

    LOG.info(
        "Rendering team invitation email for team: {}, locale: {}",
        metadata.getTeamId(),
        effectiveLocale);

    final String htmlContent =
        teamInvitationTemplate
            .data(TEMPLATE_VAR_LOCALE, effectiveLocale)
            .data(TEMPLATE_VAR_BASE_URL, frontendUrl)
            .data(TEMPLATE_VAR_TEAM_NAME, metadata.getTeamName())
            .data(TEMPLATE_VAR_INVITER_NAME, metadata.getInviterName())
            .data(TEMPLATE_VAR_ACCEPT_URL, acceptUrl)
            .data(TEMPLATE_VAR_MSG, messages)
            .render();

    final String subject = messages.teamInvitationSubject();

    return EmailRequest.builder()
        .to(recipientEmail)
        .subject(subject)
        .htmlBody(htmlContent)
        .textBody(buildPlainTextVersion(metadata, acceptUrl, messages))
        .build();
  }

  private String resolveLocale(final String locale) {
    if (locale == null || locale.isBlank()) {
      return DEFAULT_LOCALE;
    }
    final String normalizedLocale = locale.toLowerCase(Locale.ROOT).trim();
    if (LOCALE_ES.equals(normalizedLocale) || normalizedLocale.startsWith(LOCALE_ES)) {
      return LOCALE_ES;
    }
    return DEFAULT_LOCALE;
  }

  private EmailMessages getMessages(final String locale) {
    return LOCALE_ES.equalsIgnoreCase(locale) ? messagesEs : messagesEn;
  }

  private String buildAcceptUrl(final String teamId) {
    return frontendUrl + String.format(ACCEPT_INVITATION_PATH, teamId);
  }

  private String buildPlainTextVersion(
      final EmailMetadata metadata, final String acceptUrl, final EmailMessages messages) {

    return String.format(
        """
            %s

            %s

            %s: %s
            %s: %s

            %s

            %s

            %s

            %s

            ---
            %s
            """,
        messages.teamInvitationGreeting(),
        stripHtml(messages.teamInvitationIntro()),
        messages.teamInvitationTeamName(),
        metadata.getTeamName(),
        messages.teamInvitationInvitedBy(),
        metadata.getInviterName(),
        messages.teamInvitationBody(),
        messages.teamInvitationCtaIntro(),
        acceptUrl,
        messages.teamInvitationClosing() + "\n" + messages.teamInvitationSignature(),
        messages.commonFooterText());
  }

  private String stripHtml(final String html) {
    return html.replaceAll("<[^>]*>", "");
  }

  /**
   * Generic method to render any template with provided data.
   *
   * @param template the email template enum
   * @param data template variables
   * @param locale the locale
   * @return rendered HTML content
   */
  public String renderTemplate(
      final EmailTemplate template, final Map<String, Object> data, final String locale) {

    final String effectiveLocale = resolveLocale(locale);
    final EmailMessages messages = getMessages(effectiveLocale);
    final Template quteTemplate = getTemplateByType(template);

    if (quteTemplate == null) {
      if (LOG.isErrorEnabled()) {
        LOG.error("Template not found: {}", template.getTemplateName());
      }
      throw new IllegalArgumentException("Template not found: " + template.getTemplateName());
    }

    TemplateInstance instance =
        quteTemplate
            .data(TEMPLATE_VAR_LOCALE, effectiveLocale)
            .data(TEMPLATE_VAR_BASE_URL, frontendUrl)
            .data(TEMPLATE_VAR_MSG, messages);

    for (final Map.Entry<String, Object> entry : data.entrySet()) {
      instance = instance.data(entry.getKey(), entry.getValue());
    }

    return instance.render();
  }

  private Template getTemplateByType(final EmailTemplate template) {
    if (template == EmailTemplate.TEAM_INVITATION) {
      return teamInvitationTemplate;
    }
    return null;
  }

  /** Gets the subject for a template based on locale. */
  public String getSubject(final EmailTemplate template, final String locale) {
    final EmailMessages messages = getMessages(resolveLocale(locale));

    if (template == EmailTemplate.TEAM_INVITATION) {
      return messages.teamInvitationSubject();
    }
    return DEFAULT_SUBJECT;
  }
}
