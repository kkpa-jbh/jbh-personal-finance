package com.jbh.preferences.contracts;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Simplified DTO containing team preferences data for external consumers.
 * This record provides a clean data transfer object without dependencies
 * on internal domain types.
 */
public record TeamPreferencesData(
    UUID teamId,
    String currencyCode,
    BigDecimal savingsGoal,
    UUID lastModifiedBy) {

  private static final String DEFAULT_CURRENCY = "COP";

  public static TeamPreferencesData defaultFor(final UUID teamId) {
    return new TeamPreferencesData(
        teamId,
        DEFAULT_CURRENCY,
        BigDecimal.ZERO,
        null);
  }
}
