package com.jbh.notification.infra.service;

import com.jbh.commons.util.JbhJsonUtils;
import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationRequest;
import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.contracts.validation.NotificationValidationException;
import com.jbh.notification.contracts.validation.NotificationValidator;
import com.jbh.notification.infra.dto.NotificationDTO;
import com.jbh.notification.infra.email.EmailMetadata;
import com.jbh.notification.infra.email.EmailRequest;
import com.jbh.notification.infra.email.EmailTemplateService;
import com.jbh.notification.infra.persistence.NotificationStatus;
import com.jbh.notification.infra.ports.input.NotificationServicePort;
import com.jbh.notification.infra.ports.output.EmailSenderPort;
import com.jbh.notification.infra.ports.output.NotificationRepository;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Service for processing and sending notifications. */
@ApplicationScoped
public class NotificationService implements NotificationServicePort {

  private static final Logger LOG = LoggerFactory.getLogger(NotificationService.class);
  private static final String DEFAULT_SUBJECT = "JBH Notification";

  private final NotificationRepository notificationRepository;
  private final EmailSenderPort emailSenderPort;
  private final EmailTemplateService emailTemplateService;

  @Inject
  public NotificationService(
      final NotificationRepository notificationRepository,
      final EmailSenderPort emailSenderPort,
      final EmailTemplateService emailTemplateService) {
    this.notificationRepository = notificationRepository;
    this.emailSenderPort = emailSenderPort;
    this.emailTemplateService = emailTemplateService;
  }

  /**
   * Sends a notification of the specified type.
   *
   * @param request the notification data
   * @param type the type of notification to send
   * @return the saved notification
   */
  @Override
  public NotificationDTO sendNotification(
      final SendNotificationRequest request, final NotificationType type)
      throws NotificationValidationException {

    NotificationValidator.validate(type, request);

    final NotificationDTO notification = mapRequestToDTO(request);
    if (LOG.isInfoEnabled()) {
      LOG.info("Processing {} notification for recipient: {}", type, notification.recipientEmail());
    }

    final EmailMetadata metadata = EmailMetadata.fromMap(notification.metadata());
    final String subject = determineSubject(notification, metadata.getLocale());

    final NotificationDTO notificationToSave =
        NotificationDTO.builder()
            .recipientId(notification.recipientId())
            .recipientEmail(notification.recipientEmail())
            .senderUserId(notification.senderUserId())
            .senderEmail(notification.senderEmail())
            .subject(subject)
            .message(notification.message())
            .notificationType(type)
            .status(NotificationStatus.PENDING)
            .emailTemplate(notification.emailTemplate())
            .metadata(notification.metadata())
            .build();

    final NotificationDTO savedNotification = notificationRepository.save(notificationToSave);
    if (LOG.isInfoEnabled()) {
      LOG.info("Notification saved with ID: {}", savedNotification.id());
    }

    return processNotificationByType(savedNotification, type);
  }

  private NotificationDTO mapRequestToDTO(final SendNotificationRequest request) {
    return NotificationDTO.builder()
        .recipientId(request.recipientId())
        .recipientEmail(request.recipientEmail())
        .senderUserId(request.senderUserId())
        .senderEmail(request.senderEmail())
        .subject(request.subject())
        .message(request.message())
        .emailTemplate(request.emailTemplate())
        .metadata(request.metadata())
        .build();
  }

  private NotificationDTO processNotificationByType(
      final NotificationDTO notification, final NotificationType type) {

    return switch (type) {
      case EMAIL -> sendEmailNotification(notification);
      case SMS -> handleSmsNotification(notification);
      case PUSH -> handlePushNotification(notification);
      case IN_APP -> handleInAppNotification(notification);
    };
  }

  private NotificationDTO sendEmailNotification(final NotificationDTO notification) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Sending EMAIL notification to: {}", notification.recipientEmail());
    }

    final EmailRequest emailRequest = buildEmailRequest(notification);

    emailSenderPort
        .sendAsync(emailRequest)
        .emitOn(Infrastructure.getDefaultWorkerPool())
        .subscribe()
        .with(
            success -> handleEmailSuccess(notification.id()),
            failure -> handleEmailFailure(notification.id(), failure));

    return notification;
  }

  private void handleEmailSuccess(final UUID notificationId) {
    if (LOG.isInfoEnabled()) {
      LOG.info("Email sent successfully for notification: {}", notificationId);
    }
    updateNotificationStatus(notificationId, NotificationStatus.SENT);
  }

  private void handleEmailFailure(final UUID notificationId, final Throwable failure) {
    if (LOG.isErrorEnabled()) {
      LOG.error(
          "Failed to send email for notification: {}. Error: {}",
          notificationId,
          failure.getMessage());
    }
    updateNotificationStatus(notificationId, NotificationStatus.FAILED);
  }

  private EmailRequest buildEmailRequest(final NotificationDTO notification) {
    final EmailTemplate template = notification.emailTemplate();
    final EmailMetadata metadata = EmailMetadata.fromMap(notification.metadata());
    LOG.info("Building email template: {}", template);
    if (template != null) {
      return buildTemplatedEmail(notification, template, metadata);
    }

    return buildSimpleEmail(notification, metadata.getLocale());
  }

  private EmailRequest buildTemplatedEmail(
      final NotificationDTO notification,
      final EmailTemplate template,
      final EmailMetadata metadata) {

    return switch (template) {
      case TEAM_INVITATION ->
          emailTemplateService.renderTeamInvitation(notification.recipientEmail(), metadata);
      default -> buildSimpleEmail(notification, metadata.getLocale());
    };
  }

  private EmailRequest buildSimpleEmail(final NotificationDTO notification, final String lang) {
    LOG.info("Building Simple Email {}", lang);
    return EmailRequest.withTextFallback(
        notification.recipientEmail(),
        JbhJsonUtils.getField(notification.subject(), lang),
        wrapInSimpleHtml(notification.message()),
        JbhJsonUtils.getField(notification.message(), lang));
  }

  private String wrapInSimpleHtml(final String text) {
    if (text == null) {
      return "";
    }
    return String.format(
        """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
            </head>
            <body style="font-family: Arial, sans-serif; padding: 20px; color: #2d3748;">
                <p>%s</p>
                <p style="color: #667eea; font-weight: bold;">The JBH Team</p>
            </body>
            </html>
            """,
        text.replace("\n", "<br>"));
  }

  private NotificationDTO handleSmsNotification(final NotificationDTO notification) {
    if (LOG.isWarnEnabled()) {
      LOG.warn("SMS notification type not yet implemented. Notification ID: {}", notification.id());
    }
    return notification;
  }

  private NotificationDTO handlePushNotification(final NotificationDTO notification) {
    if (LOG.isWarnEnabled()) {
      LOG.warn(
          "PUSH notification type not yet implemented. Notification ID: {}", notification.id());
    }
    return notification;
  }

  private NotificationDTO handleInAppNotification(final NotificationDTO notification) {
    if (LOG.isInfoEnabled()) {
      LOG.info("IN_APP notification created with ID: {}", notification.id());
    }
    updateNotificationStatus(notification.id(), NotificationStatus.SENT);

    return NotificationDTO.builder()
        .id(notification.id())
        .recipientId(notification.recipientId())
        .recipientEmail(notification.recipientEmail())
        .senderUserId(notification.senderUserId())
        .senderEmail(notification.senderEmail())
        .subject(notification.subject())
        .message(notification.message())
        .notificationType(notification.notificationType())
        .status(NotificationStatus.SENT)
        .emailTemplate(notification.emailTemplate())
        .metadata(notification.metadata())
        .sentAt(LocalDateTime.now())
        .createdAt(notification.createdAt())
        .updatedAt(notification.updatedAt())
        .build();
  }

  private void updateNotificationStatus(
      final UUID notificationId, final NotificationStatus status) {
    try {
      notificationRepository.updateStatus(notificationId, status.name());
    } catch (final IllegalArgumentException | IllegalStateException e) {
      if (LOG.isErrorEnabled()) {
        LOG.error(
            "Failed to update notification {} status to {}. Error: {}",
            notificationId,
            status,
            e.getMessage());
      }
    }
  }

  private String determineSubject(final NotificationDTO notification, final String locale) {
    final EmailTemplate template = notification.emailTemplate();
    if (template != null) {
      return emailTemplateService.getSubject(template, locale);
    }

    if (notification.subject() != null && !notification.subject().isBlank()) {
      return notification.subject();
    }

    return DEFAULT_SUBJECT;
  }
}
