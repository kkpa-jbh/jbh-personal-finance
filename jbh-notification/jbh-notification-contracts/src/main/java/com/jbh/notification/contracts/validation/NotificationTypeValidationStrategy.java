package com.jbh.notification.contracts.validation;

import com.jbh.notification.contracts.SendNotificationCommand;
import java.util.List;

public interface NotificationTypeValidationStrategy {

  List<ValidationError> validate(SendNotificationCommand request);
}
