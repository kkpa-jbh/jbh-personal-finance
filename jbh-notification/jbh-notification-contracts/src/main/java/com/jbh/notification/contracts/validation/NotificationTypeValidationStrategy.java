package com.jbh.notification.contracts.validation;

import com.jbh.notification.contracts.SendNotificationRequest;
import java.util.List;

public interface NotificationTypeValidationStrategy {

    List<ValidationError> validate(SendNotificationRequest request);
}
