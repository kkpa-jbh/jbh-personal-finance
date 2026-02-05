package com.jbh.preferences.application.core.vo.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdateTeamPreferencesCommand(
    Currency defaultCurrency,
    BigDecimal savingsGoal,
    PreferencesMetadata metadata,
    UUID lastModifiedBy) {

  @SuppressWarnings("PMD.UnusedAssignment")
  public UpdateTeamPreferencesCommand {
    if (savingsGoal != null && savingsGoal.signum() < 0) {
      throw new GenericSpecificationException("Savings goal cannot be negative");
    }
    if (defaultCurrency == null) {
      defaultCurrency = Currency.defaultCurrency();
    }
    if (metadata == null) {
      metadata = PreferencesMetadata.empty();
    }
  }

  public static UpdateTeamPreferencesCommandBuilder builder() {
    return new UpdateTeamPreferencesCommandBuilder();
  }

  @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
  public static class UpdateTeamPreferencesCommandBuilder {
    private Currency currencyValue = Currency.defaultCurrency();
    private BigDecimal goalValue;
    private PreferencesMetadata metadataValue = PreferencesMetadata.empty();
    private UUID lastModifiedByValue;

    public UpdateTeamPreferencesCommandBuilder defaultCurrency(final Currency currency) {
      this.currencyValue = currency;
      return this;
    }

    public UpdateTeamPreferencesCommandBuilder savingsGoal(final BigDecimal goal) {
      this.goalValue = goal;
      return this;
    }

    public UpdateTeamPreferencesCommandBuilder metadata(final PreferencesMetadata meta) {
      this.metadataValue = meta;
      return this;
    }

    public UpdateTeamPreferencesCommandBuilder lastModifiedBy(final UUID userId) {
      this.lastModifiedByValue = userId;
      return this;
    }

    public UpdateTeamPreferencesCommand build() {
      return new UpdateTeamPreferencesCommand(
          currencyValue, goalValue, metadataValue, lastModifiedByValue);
    }
  }
}
