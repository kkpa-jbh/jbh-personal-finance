package com.jbh.notification.contracts.validation;

import com.jbh.notification.contracts.SendNotificationCommand;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public abstract class BaseNotificationValidationStrategy
    implements NotificationTypeValidationStrategy {

  private static final Pattern EMAIL_PATTERN =
      Pattern.compile("^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

  protected List<ValidationError> validateCommonFields(final SendNotificationCommand request) {
    final List<ValidationError> errors = new ArrayList<>();

    if (request.recipientId() == null) {
      errors.add(ValidationError.required("recipientId"));
    }

    if (request.recipientEmail() == null) {
      errors.add(ValidationError.required("recipientEmail"));
    } else if (request.recipientEmail().isBlank()) {
      errors.add(ValidationError.blank("recipientEmail", request.recipientEmail()));
    } else if (!isValidEmail(request.recipientEmail())) {
      errors.add(ValidationError.invalidEmail("recipientEmail", request.recipientEmail()));
    }

    if (request.senderEmail() != null && !request.senderEmail().isBlank()) {
      if (!isValidEmail(request.senderEmail())) {
        errors.add(ValidationError.invalidEmail("senderEmail", request.senderEmail()));
      }
    }

    return errors;
  }

  protected boolean isValidEmail(final String email) {
    return email != null && EMAIL_PATTERN.matcher(email).matches();
  }

  protected boolean isBlank(final String value) {
    return value == null || value.isBlank();
  }
}
