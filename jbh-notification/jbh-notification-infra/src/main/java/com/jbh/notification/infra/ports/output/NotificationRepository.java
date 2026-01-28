package com.jbh.notification.infra.ports.output;

import com.jbh.notification.infra.dto.NotificationDTO;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    NotificationDTO save(NotificationDTO notification);

    Optional<NotificationDTO> findById(UUID id);

    List<NotificationDTO> findByRecipientId(UUID recipientId);

    void updateStatus(UUID id, String status);
}
