package com.jbh.notification.infra.adapters.out.persistence;

import com.jbh.notification.infra.dto.NotificationDTO;
import com.jbh.notification.infra.persistence.NotificationEntity;
import com.jbh.notification.infra.persistence.NotificationStatus;
import com.jbh.notification.infra.ports.output.NotificationRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
@SuppressWarnings({"PMD.CloseResource", "PMD.LawOfDemeter"})
public class NotificationRepositoryAdapter implements NotificationRepository {

    private static final Logger LOG = LoggerFactory.getLogger(NotificationRepositoryAdapter.class);

    private final NotificationJPARepository jpaRepository;

    @Inject
    public NotificationRepositoryAdapter(final NotificationJPARepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    @Transactional
    public NotificationDTO save(final NotificationDTO notification) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Saving notification for recipient: {}", notification.recipientId());
        }

        final NotificationEntity entity = NotificationEntity.toEntity(notification);

        if (entity.getId() == null) {
            jpaRepository.persist(entity);
            if (LOG.isInfoEnabled()) {
                LOG.info("Created new notification with ID: {}", entity.getId());
            }
        } else {
            final EntityManager entityManager = jpaRepository.getEntityManager();
            entityManager.merge(entity);
            if (LOG.isInfoEnabled()) {
                LOG.info("Updated notification with ID: {}", entity.getId());
            }
        }

        return entity.toDTO();
    }

    @Override
    public Optional<NotificationDTO> findById(final UUID id) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding notification by ID: {}", id);
        }
        return jpaRepository.findByNotificationId(id).map(NotificationEntity::toDTO);
    }

    @Override
    public List<NotificationDTO> findByRecipientId(final UUID recipientId) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Finding notifications for recipient: {}", recipientId);
        }
        return jpaRepository.findByRecipientId(recipientId)
            .stream()
            .map(NotificationEntity::toDTO)
            .toList();
    }

    @Override
    @Transactional
    public void updateStatus(final UUID id, final String status) {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Updating notification {} status to: {}", id, status);
        }

        jpaRepository.findByNotificationId(id).ifPresent(entity -> {
            entity.setStatus(NotificationStatus.valueOf(status));
            final EntityManager entityManager = jpaRepository.getEntityManager();
            entityManager.merge(entity);
            if (LOG.isInfoEnabled()) {
                LOG.info("Updated notification {} status to {}", id, status);
            }
        });
    }
}
