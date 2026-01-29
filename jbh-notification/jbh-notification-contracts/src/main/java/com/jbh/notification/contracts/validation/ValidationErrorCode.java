package com.jbh.notification.contracts.validation;

public enum ValidationErrorCode {
    REQUIRED("Field is required"),
    INVALID_FORMAT("Field has invalid format"),
    INVALID_EMAIL("Email address is not valid"),
    MISSING_METADATA_KEY("Required metadata key is missing"),
    BLANK_VALUE("Field cannot be blank");

    private final String defaultMessage;

    ValidationErrorCode(final String defaultMessage) {
        this.defaultMessage = defaultMessage;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
