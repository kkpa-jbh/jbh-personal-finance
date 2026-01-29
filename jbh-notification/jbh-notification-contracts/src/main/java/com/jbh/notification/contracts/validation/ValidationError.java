package com.jbh.notification.contracts.validation;

public record ValidationError(
    String field,
    ValidationErrorCode code,
    String message,
    Object rejectedValue) {

    public static ValidationError required(final String field) {
        return new ValidationError(
            field,
            ValidationErrorCode.REQUIRED,
            String.format("%s is required", field),
            null);
    }

    public static ValidationError blank(final String field, final Object rejectedValue) {
        return new ValidationError(
            field,
            ValidationErrorCode.BLANK_VALUE,
            String.format("%s cannot be blank", field),
            rejectedValue);
    }

    public static ValidationError invalidFormat(final String field, final Object rejectedValue, final String expectedFormat) {
        return new ValidationError(
            field,
            ValidationErrorCode.INVALID_FORMAT,
            String.format("%s has invalid format. Expected: %s", field, expectedFormat),
            rejectedValue);
    }

    public static ValidationError invalidEmail(final String field, final Object rejectedValue) {
        return new ValidationError(
            field,
            ValidationErrorCode.INVALID_EMAIL,
            String.format("%s must be a valid email address", field),
            rejectedValue);
    }

    public static ValidationError missingMetadataKey(final String key, final String templateName) {
        return new ValidationError(
            "metadata." + key,
            ValidationErrorCode.MISSING_METADATA_KEY,
            String.format("Missing required metadata key '%s' for template %s", key, templateName),
            null);
    }
}
