package com.jbh.notification.contracts.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.jbh.notification.contracts.NotificationType;
import com.jbh.notification.contracts.SendNotificationRequest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class NotificationValidatorTest {

    @Test
    void shouldValidateSuccessfullyWithValidRequest() throws NotificationValidationException {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        NotificationValidator.validate(NotificationType.EMAIL, request);
    }

    @Test
    void shouldThrowExceptionWhenValidationFails() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        assertThatThrownBy(() -> NotificationValidator.validate(NotificationType.EMAIL, request))
            .isInstanceOf(NotificationValidationException.class)
            .satisfies(ex -> {
                final NotificationValidationException validationEx = (NotificationValidationException) ex;
                assertThat(validationEx.getErrors()).isNotEmpty();
                assertThat(validationEx.getErrors()).anyMatch(e -> e.field().equals("recipientId"));
            });
    }

    @Test
    void shouldReturnErrorListWithValidateAndCollect() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientEmail("not-valid-email")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        final List<ValidationError> errors = NotificationValidator.validateAndCollect(
            NotificationType.EMAIL, request);

        assertThat(errors).isNotEmpty();
        assertThat(errors).anyMatch(e -> e.field().equals("recipientId"));
        assertThat(errors).anyMatch(e -> e.field().equals("recipientEmail"));
    }

    @Test
    void shouldReturnEmptyListWhenValid() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        final List<ValidationError> errors = NotificationValidator.validateAndCollect(
            NotificationType.EMAIL, request);

        assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnTrueForValidRequest() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        final boolean isValid = NotificationValidator.isValid(NotificationType.EMAIL, request);

        assertThat(isValid).isTrue();
    }

    @Test
    void shouldReturnFalseForInvalidRequest() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        final boolean isValid = NotificationValidator.isValid(NotificationType.EMAIL, request);

        assertThat(isValid).isFalse();
    }

    @Test
    void shouldReturnErrorWhenRequestIsNull() {
        final List<ValidationError> errors = NotificationValidator.validateAndCollect(
            NotificationType.EMAIL, null);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).field()).isEqualTo("request");
        assertThat(errors.get(0).code()).isEqualTo(ValidationErrorCode.REQUIRED);
    }

    @Test
    void shouldReturnErrorWhenNotificationTypeIsNull() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        final List<ValidationError> errors = NotificationValidator.validateAndCollect(null, request);

        assertThat(errors).hasSize(1);
        assertThat(errors.get(0).field()).isEqualTo("notificationType");
    }

    @Test
    void shouldThrowUnsupportedOperationForUnimplementedType() {
        final SendNotificationRequest request = SendNotificationRequest.builder()
            .recipientId(UUID.randomUUID())
            .recipientEmail("test@example.com")
            .subject("Test Subject")
            .message("Test Message")
            .build();

        assertThatThrownBy(() -> NotificationValidator.validate(NotificationType.SMS, request))
            .isInstanceOf(UnsupportedOperationException.class)
            .hasMessageContaining("SMS");
    }
}
