package com.jbh.notification.infra.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", schema = "notifications")
public class NotificationEntity extends PanacheEntity {
    
    @Column(name = "recipient", nullable = false)
    public String recipient;
    
    @Column(name = "subject", nullable = false)
    public String subject;
    
    @Column(name = "message", columnDefinition = "TEXT")
    public String message;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    public NotificationType notificationType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    public NotificationStatus status = NotificationStatus.PENDING;
    
    @Column(name = "sent_at")
    public LocalDateTime sentAt;
    
    @Column(name = "created_at")
    public LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    public LocalDateTime updatedAt;
    
    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum NotificationType {
        EMAIL, SMS, PUSH, IN_APP
    }
    
    public enum NotificationStatus {
        PENDING, SENT, FAILED, CANCELLED
    }
}