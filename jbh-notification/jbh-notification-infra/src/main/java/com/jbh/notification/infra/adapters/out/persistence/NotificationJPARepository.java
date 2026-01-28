package com.jbh.notification.infra.adapters.out.persistence;

import com.jbh.notification.infra.persistence.NotificationEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class NotificationJPARepository implements PanacheRepositoryBase<NotificationEntity, UUID> {

    public Optional<NotificationEntity> findByNotificationId(final UUID id) {
        return find("id", id).firstResultOptional();
    }

    public List<NotificationEntity> findByRecipientId(final UUID recipientId) {
        return find("recipientId", recipientId).list();
    }
}
