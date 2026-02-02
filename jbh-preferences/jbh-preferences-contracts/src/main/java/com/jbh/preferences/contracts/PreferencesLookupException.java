package com.jbh.preferences.contracts;

import java.util.UUID;

/**
 * Exception thrown when preferences lookup operations fail.
 * Provides a clean exception boundary for consuming modules.
 */
public class PreferencesLookupException extends Exception {

  private final UUID userId;

  public PreferencesLookupException(final String message) {
    super(message);
    this.userId = null;
  }

  public PreferencesLookupException(final String message, final UUID userId) {
    super(message);
    this.userId = userId;
  }

  public PreferencesLookupException(final String message, final Throwable cause) {
    super(message, cause);
    this.userId = null;
  }

  public PreferencesLookupException(final String message, final UUID userId, final Throwable cause) {
    super(message, cause);
    this.userId = userId;
  }

  public UUID getUserId() {
    return userId;
  }

  public boolean hasUserId() {
    return userId != null;
  }
}
