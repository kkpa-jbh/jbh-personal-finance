package com.jbh.notification.contracts.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationValidationExceptionTest {

    @Test
    void shouldCreateExceptionWithSingleError() {
        final ValidationError error = ValidationError.required("recipientId");

        final NotificationValidationException exception = new NotificationValidationException(error);

        assertThat(exception.getErrors()).hasSize(1);
        assertThat(exception.getErrorCount()).isEqualTo(1);
        assertThat(exception.hasErrors()).isTrue();
        assertThat(exception.getMessage()).isEqualTo("Validation failed: recipientId is required");
    }

    @Test
    void shouldCreateExceptionWithMultipleErrors() {
        final List<ValidationError> errors = List.of(
            ValidationError.required("recipientId"),
            ValidationError.required("recipientEmail"),
            ValidationError.invalidEmail("senderEmail", "bad-email")
        );

        final NotificationValidationException exception = new NotificationValidationException(errors);

        assertThat(exception.getErrors()).hasSize(3);
        assertThat(exception.getErrorCount()).isEqualTo(3);
        assertThat(exception.hasErrors()).isTrue();
        assertThat(exception.getMessage())
            .startsWith("Validation failed with 3 errors:")
            .contains("recipientId is required")
            .contains("recipientEmail is required")
            .contains("senderEmail must be a valid email address");
    }

    @Test
    void shouldReturnUnmodifiableErrorList() {
        final ValidationError error = ValidationError.required("recipientId");
        final NotificationValidationException exception = new NotificationValidationException(error);

        final List<ValidationError> errors = exception.getErrors();

        assertThat(errors).isUnmodifiable();
    }

    @Test
    void shouldHandleEmptyErrorList() {
        final NotificationValidationException exception = new NotificationValidationException(List.of());

        assertThat(exception.getErrors()).isEmpty();
        assertThat(exception.getErrorCount()).isZero();
        assertThat(exception.hasErrors()).isFalse();
        assertThat(exception.getMessage()).isEqualTo("Validation failed");
    }
}
