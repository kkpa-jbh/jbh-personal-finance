package com.jbh.preferences.contracts;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simplified DTO containing user preferences data for external consumers.
 * This record provides a clean data transfer object without dependencies
 * on internal domain types.
 */
public record UserPreferencesData(
    UUID userId,
    String languageCode,
    String currencyCode,
    BigDecimal savingsGoal,
    UUID defaultAccountId) {

  private static final String DEFAULT_LANGUAGE = "es";
  private static final String DEFAULT_CURRENCY = "USD";

  public static UserPreferencesData defaultFor(final UUID userId) {
    return new UserPreferencesData(
        userId,
        DEFAULT_LANGUAGE,
        DEFAULT_CURRENCY,
        BigDecimal.ZERO,
        null);
  }

  public boolean hasDefaultAccount() {
    return defaultAccountId != null;
  }
}
