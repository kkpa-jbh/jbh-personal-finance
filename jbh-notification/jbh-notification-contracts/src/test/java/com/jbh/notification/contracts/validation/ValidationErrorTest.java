package com.jbh.notification.contracts.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ValidationErrorTest {

    @Test
    void shouldCreateRequiredError() {
        final ValidationError error = ValidationError.required("recipientId");

        assertThat(error.field()).isEqualTo("recipientId");
        assertThat(error.code()).isEqualTo(ValidationErrorCode.REQUIRED);
        assertThat(error.message()).isEqualTo("recipientId is required");
        assertThat(error.rejectedValue()).isNull();
    }

    @Test
    void shouldCreateBlankError() {
        final ValidationError error = ValidationError.blank("userName", "   ");

        assertThat(error.field()).isEqualTo("userName");
        assertThat(error.code()).isEqualTo(ValidationErrorCode.BLANK_VALUE);
        assertThat(error.message()).isEqualTo("userName cannot be blank");
        assertThat(error.rejectedValue()).isEqualTo("   ");
    }

    @Test
    void shouldCreateInvalidFormatError() {
        final ValidationError error = ValidationError.invalidFormat("date", "not-a-date", "yyyy-MM-dd");

        assertThat(error.field()).isEqualTo("date");
        assertThat(error.code()).isEqualTo(ValidationErrorCode.INVALID_FORMAT);
        assertThat(error.message()).isEqualTo("date has invalid format. Expected: yyyy-MM-dd");
        assertThat(error.rejectedValue()).isEqualTo("not-a-date");
    }

    @Test
    void shouldCreateInvalidEmailError() {
        final ValidationError error = ValidationError.invalidEmail("recipientEmail", "not-an-email");

        assertThat(error.field()).isEqualTo("recipientEmail");
        assertThat(error.code()).isEqualTo(ValidationErrorCode.INVALID_EMAIL);
        assertThat(error.message()).isEqualTo("recipientEmail must be a valid email address");
        assertThat(error.rejectedValue()).isEqualTo("not-an-email");
    }

    @Test
    void shouldCreateMissingMetadataKeyError() {
        final ValidationError error = ValidationError.missingMetadataKey("teamId", "TEAM_INVITATION");

        assertThat(error.field()).isEqualTo("metadata.teamId");
        assertThat(error.code()).isEqualTo(ValidationErrorCode.MISSING_METADATA_KEY);
        assertThat(error.message()).isEqualTo("Missing required metadata key 'teamId' for template TEAM_INVITATION");
        assertThat(error.rejectedValue()).isNull();
    }
}
