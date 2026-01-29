package com.jbh.notification.contracts.validation;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class NotificationValidationException extends Exception {

  private final List<ValidationError> errors;

  public NotificationValidationException(final ValidationError error) {
    this(List.of(error));
  }

  public NotificationValidationException(final List<ValidationError> errors) {
    super(buildMessage(errors));
    this.errors = Collections.unmodifiableList(errors);
  }

  private static String buildMessage(final List<ValidationError> errors) {
    if (errors == null || errors.isEmpty()) {
      return "Validation failed";
    }
    if (errors.size() == 1) {
      return "Validation failed: " + errors.get(0).message();
    }
    final String errorMessages =
        errors.stream().map(ValidationError::message).collect(Collectors.joining("; "));
    return String.format("Validation failed with %d errors: %s", errors.size(), errorMessages);
  }

  public List<ValidationError> getErrors() {
    return errors;
  }

  public boolean hasErrors() {
    return !errors.isEmpty();
  }

  public int getErrorCount() {
    return errors.size();
  }
}
