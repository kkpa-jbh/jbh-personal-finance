package com.jbh.notification.infra.persistence;

import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.email.EmailTemplate;
import com.jbh.notification.infra.dto.NotificationDTO;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.Type;

@Entity
@Getter
@Setter
@Table(name = "notification", schema = "notifications")
public class NotificationEntity extends PanacheEntityBase {

  @Id
  @Column(name = "id")
  private UUID id;

  @Column(name = "recipient_id", nullable = false)
  private UUID recipientId;

  @Column(name = "recipient_email", nullable = false, length = 255)
  private String recipientEmail;

  @Column(name = "sender_user_id")
  private UUID senderUserId;

  @Column(name = "sender_email", length = 255)
  private String senderEmail;

  @Column(name = "subject", nullable = false, length = 500)
  private String subject;

  @Column(name = "message", columnDefinition = "TEXT")
  private String message;

  @Enumerated(EnumType.STRING)
  @Column(name = "notification_type", nullable = false, length = 50)
  private NotificationType notificationType;

  @Enumerated(EnumType.STRING)
  @Column(name = "template", length = 50)
  private EmailTemplate template;

  @Enumerated(EnumType.STRING)
  @Column(name = "status", nullable = false, length = 50)
  private NotificationStatus status = NotificationStatus.PENDING;

  @Type(JsonBinaryType.class)
  @Column(name = "metadata", columnDefinition = "jsonb")
  private Map<String, Object> metadata;

  @Column(name = "sent_at")
  private LocalDateTime sentAt;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static NotificationEntity toEntity(final NotificationDTO dto) {
    final NotificationEntity entity = new NotificationEntity();
    entity.setId(dto.id());
    entity.setRecipientId(dto.recipientId());
    entity.setRecipientEmail(dto.recipientEmail());
    entity.setSenderUserId(dto.senderUserId());
    entity.setSenderEmail(dto.senderEmail());
    entity.setSubject(dto.subject());
    entity.setMessage(dto.message());
    entity.setNotificationType(dto.notificationType());
    entity.setTemplate(dto.emailTemplate());
    entity.setStatus(dto.status() != null ? dto.status() : NotificationStatus.PENDING);
    entity.setMetadata(dto.metadata());
    entity.setSentAt(dto.sentAt());
    entity.setCreatedAt(dto.createdAt());
    entity.setUpdatedAt(dto.updatedAt());
    return entity;
  }

  @PrePersist
  protected void onCreate() {
    if (id == null) {
      id = UUID.randomUUID();
    }
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public NotificationDTO toDTO() {
    return NotificationDTO.builder()
        .id(id)
        .recipientId(recipientId)
        .recipientEmail(recipientEmail)
        .senderUserId(senderUserId)
        .senderEmail(senderEmail)
        .subject(subject)
        .message(message)
        .notificationType(notificationType)
        .emailTemplate(template)
        .status(status)
        .metadata(metadata)
        .sentAt(sentAt)
        .createdAt(createdAt)
        .updatedAt(updatedAt)
        .build();
  }
}
