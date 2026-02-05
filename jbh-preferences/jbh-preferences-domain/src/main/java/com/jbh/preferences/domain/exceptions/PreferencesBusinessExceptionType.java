package com.jbh.preferences.domain.exceptions;

import com.jbh.commons.exception.BusinessExceptionType;

public enum PreferencesBusinessExceptionType implements BusinessExceptionType {
  PREFERENCES_NOT_FOUND("User preferences not found for user ID: %s"),
  PREFERENCES_ALREADY_EXIST("User preferences already exist for user ID: %s"),
  TEAM_PREFERENCES_NOT_FOUND("Team preferences not found for team ID: %s"),
  TEAM_PREFERENCES_ALREADY_EXIST("Team preferences already exist for team ID: %s"),
  INVALID_SAVINGS_GOAL("Savings goal cannot be negative"),
  INVALID_LANGUAGE_CODE("Invalid language code: %s"),
  INVALID_CURRENCY_CODE("Invalid currency code: %s");

  private final String message;

  PreferencesBusinessExceptionType(final String message) {
    this.message = message;
  }

  @Override
  public String getMessage() {
    return message;
  }
}
