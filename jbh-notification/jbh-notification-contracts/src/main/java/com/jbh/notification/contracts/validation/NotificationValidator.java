package com.jbh.notification.contracts.validation;

import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationCommand;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class NotificationValidator {

  private static final Map<NotificationType, NotificationTypeValidationStrategy> STRATEGIES;

  static {
    final Map<NotificationType, NotificationTypeValidationStrategy> strategies =
        new EnumMap<>(NotificationType.class);
    strategies.put(NotificationType.EMAIL, new EmailNotificationValidationStrategy());
    STRATEGIES = strategies;
  }

  private NotificationValidator() {}

  public static void validate(final NotificationType type, final SendNotificationCommand request)
      throws NotificationValidationException {
    final List<ValidationError> errors = validateAndCollect(type, request);
    if (!errors.isEmpty()) {
      throw new NotificationValidationException(errors);
    }
  }

  public static List<ValidationError> validateAndCollect(
      final NotificationType type, final SendNotificationCommand request) {
    if (request == null) {
      return List.of(ValidationError.required("request"));
    }
    if (type == null) {
      return List.of(ValidationError.required("notificationType"));
    }

    final NotificationTypeValidationStrategy strategy = STRATEGIES.get(type);
    if (strategy == null) {
      throw new UnsupportedOperationException(
          String.format("Validation not implemented for notification type: %s", type));
    }

    return strategy.validate(request);
  }

  public static boolean isValid(
      final NotificationType type, final SendNotificationCommand request) {
    return validateAndCollect(type, request).isEmpty();
  }
}
