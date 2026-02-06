package com.jbh.preferences.application.core.vo.commands;

import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.util.UUID;

public record UpdatePreferencesCommand(
    Language defaultLang,
    Currency defaultCurrency,
    BigDecimal savingsGoal,
    UUID defaultProductId,
    PreferencesMetadata metadata) {

  @SuppressWarnings("PMD.UnusedAssignment")
  public UpdatePreferencesCommand {
    if (savingsGoal != null && savingsGoal.signum() < 0) {
      throw new GenericSpecificationException("Savings goal cannot be negative");
    }
    if (defaultLang == null) {
      defaultLang = Language.DEFAULT;
    }
    if (defaultCurrency == null) {
      defaultCurrency = Currency.defaultCurrency();
    }
    if (metadata == null) {
      metadata = PreferencesMetadata.empty();
    }
  }

  public static UpdatePreferencesCommandBuilder builder() {
    return new UpdatePreferencesCommandBuilder();
  }

  @SuppressWarnings("PMD.AvoidFieldNameMatchingMethodName")
  public static class UpdatePreferencesCommandBuilder {
    private Language langValue = Language.DEFAULT;
    private Currency currencyValue = Currency.defaultCurrency();
    private BigDecimal goalValue;
    private UUID accountIdValue;
    private PreferencesMetadata metadataValue = PreferencesMetadata.empty();

    public UpdatePreferencesCommandBuilder defaultLang(final Language lang) {
      this.langValue = lang;
      return this;
    }

    public UpdatePreferencesCommandBuilder defaultCurrency(final Currency currency) {
      this.currencyValue = currency;
      return this;
    }

    public UpdatePreferencesCommandBuilder savingsGoal(final BigDecimal goal) {
      this.goalValue = goal;
      return this;
    }

    public UpdatePreferencesCommandBuilder defaultAccountId(final UUID accountId) {
      this.accountIdValue = accountId;
      return this;
    }

    public UpdatePreferencesCommandBuilder metadata(final PreferencesMetadata meta) {
      this.metadataValue = meta;
      return this;
    }

    public UpdatePreferencesCommand build() {
      return new UpdatePreferencesCommand(
          langValue, currencyValue, goalValue, accountIdValue, metadataValue);
    }
  }
}
