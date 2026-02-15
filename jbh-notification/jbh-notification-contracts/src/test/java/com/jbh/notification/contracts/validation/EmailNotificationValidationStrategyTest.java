package com.jbh.notification.contracts.validation;

import static org.assertj.core.api.Assertions.assertThat;

import com.jbh.notification.contracts.SendNotificationCommand;
import com.jbh.notification.contracts.email.EmailTemplate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class EmailNotificationValidationStrategyTest {

  private EmailNotificationValidationStrategy strategy;

  @BeforeEach
  void setUp() {
    strategy = new EmailNotificationValidationStrategy();
  }

  @Nested
  class CommonFieldValidation {

    @Test
    void shouldReturnErrorWhenRecipientIdIsNull() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientEmail("test@example.com")
              .subject("Test")
              .message("Test message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(
              e -> e.field().equals("recipientId") && e.code() == ValidationErrorCode.REQUIRED);
    }

    @Test
    void shouldReturnErrorWhenRecipientEmailIsNull() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .subject("Test")
              .message("Test message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(
              e -> e.field().equals("recipientEmail") && e.code() == ValidationErrorCode.REQUIRED);
    }

    @Test
    void shouldReturnErrorWhenRecipientEmailIsBlank() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("   ")
              .subject("Test")
              .message("Test message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(
              e ->
                  e.field().equals("recipientEmail")
                      && e.code() == ValidationErrorCode.BLANK_VALUE);
    }

    @Test
    void shouldReturnErrorWhenRecipientEmailIsInvalid() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("not-an-email")
              .subject("Test")
              .message("Test message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(
              e ->
                  e.field().equals("recipientEmail")
                      && e.code() == ValidationErrorCode.INVALID_EMAIL);
    }

    @Test
    void shouldReturnErrorWhenSenderEmailIsInvalid() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("test@example.com")
              .senderEmail("not-an-email")
              .subject("Test")
              .message("Test message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(
              e ->
                  e.field().equals("senderEmail") && e.code() == ValidationErrorCode.INVALID_EMAIL);
    }

    @Test
    void shouldNotReturnErrorWhenSenderEmailIsNull() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("test@example.com")
              .subject("Test")
              .message("Test message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).noneMatch(e -> e.field().equals("senderEmail"));
    }
  }

  @Nested
  class NonTemplateEmailValidation {

    @Test
    void shouldValidateSuccessfullyWhenSubjectAndMessagePresent() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("test@example.com")
              .subject("Test Subject")
              .message("Test Message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnErrorWhenSubjectIsMissing() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("test@example.com")
              .message("Test Message")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(e -> e.field().equals("subject") && e.code() == ValidationErrorCode.REQUIRED);
    }

    @Test
    void shouldReturnErrorWhenMessageIsMissing() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("test@example.com")
              .subject("Test Subject")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors)
          .anyMatch(e -> e.field().equals("message") && e.code() == ValidationErrorCode.REQUIRED);
    }

    @Test
    void shouldReturnMultipleErrorsWhenBothSubjectAndMessageMissing() {
      final SendNotificationCommand request =
          SendNotificationCommand.builder()
              .recipientId(UUID.randomUUID())
              .recipientEmail("test@example.com")
              .build();

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).hasSize(2);
      assertThat(errors).anyMatch(e -> e.field().equals("subject"));
      assertThat(errors).anyMatch(e -> e.field().equals("message"));
    }
  }

  @Nested
  class TemplateEmailValidation {

    @Test
    void shouldValidateSuccessfullyWithValidTemplateMetadata() {
      final Map<String, Object> metadata =
          Map.of(
              "teamId", UUID.randomUUID(),
              "teamName", "Test Team",
              "inviterName", "John Doe");

      final SendNotificationCommand request =
          new SendNotificationCommand(
              UUID.randomUUID(),
              "test@example.com",
              null,
              null,
              null,
              null,
              EmailTemplate.TEAM_INVITATION,
              metadata);

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).isEmpty();
    }

    @Test
    void shouldReturnErrorWhenTemplateMetadataIsMissing() {
      final SendNotificationCommand request =
          new SendNotificationCommand(
              UUID.randomUUID(),
              "test@example.com",
              null,
              null,
              null,
              null,
              EmailTemplate.TEAM_INVITATION,
              Map.of());

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).isNotEmpty();
      assertThat(errors).allMatch(e -> e.code() == ValidationErrorCode.MISSING_METADATA_KEY);
    }

    @Test
    void shouldReturnErrorForEachMissingMetadataKey() {
      final SendNotificationCommand request =
          new SendNotificationCommand(
              UUID.randomUUID(),
              "test@example.com",
              null,
              null,
              null,
              null,
              EmailTemplate.TEAM_INVITATION,
              Map.of("teamId", UUID.randomUUID()));

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).hasSize(2);
      assertThat(errors).anyMatch(e -> e.field().equals("metadata.teamName"));
      assertThat(errors).anyMatch(e -> e.field().equals("metadata.inviterName"));
    }

    @Test
    void shouldValidateWelcomeTemplate() {
      final SendNotificationCommand request =
          new SendNotificationCommand(
              UUID.randomUUID(),
              "test@example.com",
              null,
              null,
              null,
              null,
              EmailTemplate.WELCOME,
              Map.of("userName", "John Doe"));

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).isEmpty();
    }

    @Test
    void shouldValidatePasswordResetTemplate() {
      final SendNotificationCommand request =
          new SendNotificationCommand(
              UUID.randomUUID(),
              "test@example.com",
              null,
              null,
              null,
              null,
              EmailTemplate.PASSWORD_RESET,
              Map.of("userName", "John Doe", "resetToken", "abc123"));

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).isEmpty();
    }

    @Test
    void shouldValidateAccountVerificationTemplate() {
      final SendNotificationCommand request =
          new SendNotificationCommand(
              UUID.randomUUID(),
              "test@example.com",
              null,
              null,
              null,
              null,
              EmailTemplate.ACCOUNT_VERIFICATION,
              Map.of("userName", "John Doe", "verificationCode", "123456"));

      final List<ValidationError> errors = strategy.validate(request);

      assertThat(errors).isEmpty();
    }
  }
}
