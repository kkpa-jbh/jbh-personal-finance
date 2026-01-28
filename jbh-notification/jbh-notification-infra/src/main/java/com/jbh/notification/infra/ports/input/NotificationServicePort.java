package com.jbh.notification.infra.ports.input;

import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.infra.dto.NotificationDTO;

/**
 * Input port for notification operations.
 * Defines the contract for notification service operations.
 */
public interface NotificationServicePort {

    /**
     * Sends a notification of the specified type.
     *
     * @param notification the notification data
     * @param type the type of notification to send (EMAIL, SMS, PUSH, IN_APP)
     * @return the saved notification with ID and status
     */
    NotificationDTO sendNotification(NotificationDTO notification, NotificationType type);
}
