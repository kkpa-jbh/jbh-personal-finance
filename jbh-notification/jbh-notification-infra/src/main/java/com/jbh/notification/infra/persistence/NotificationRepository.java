package com.jbh.notification.infra.persistence;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import io.quarkus.panache.common.Parameters;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceUnit;
import java.time.LocalDateTime;
import java.util.List;

@ApplicationScoped
@PersistenceUnit(name = "notifications")
public class NotificationRepository implements PanacheRepository<NotificationEntity> {
    
    public List<NotificationEntity> findPendingNotifications() {
        return find("status", NotificationEntity.NotificationStatus.PENDING).list();
    }
    
    public List<NotificationEntity> findByRecipient(String recipient) {
        return find("recipient", recipient).list();
    }
    
    public List<NotificationEntity> findByTypeAndStatus(
            NotificationEntity.NotificationType type, 
            NotificationEntity.NotificationStatus status) {
        return find("notificationType = :type and status = :status", 
                    Parameters.with("type", type)
                             .and("status", status)).list();
    }
    
    public void markAsSent(Long notificationId) {
        NotificationEntity notification = findById(notificationId);
        if (notification != null) {
            notification.status = NotificationEntity.NotificationStatus.SENT;
            notification.sentAt = LocalDateTime.now();
            persist(notification);
        }
    }
}