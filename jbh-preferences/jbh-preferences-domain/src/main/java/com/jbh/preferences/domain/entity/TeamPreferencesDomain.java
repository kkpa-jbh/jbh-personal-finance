package com.jbh.preferences.domain.entity;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.isNegative;

import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderMethodName = "internalBuilder")
public final class TeamPreferencesDomain {

  private final UUID teamId;
  private final Currency defaultCurrency;
  private final BigDecimal savingsGoal;
  private final PreferencesMetadata metadata;
  private final UUID lastModifiedBy;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public static TeamPreferencesDomain createDefault(final UUID teamId) {
    Objects.requireNonNull(teamId, "Team ID cannot be null");

    return internalBuilder()
        .teamId(teamId)
        .defaultCurrency(Currency.COP)
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty())
        .lastModifiedBy(null)
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public static TeamPreferencesDomainBuilder builder(final UUID teamId) {
    Objects.requireNonNull(teamId, "Team ID cannot be null");

    return internalBuilder()
        .teamId(teamId)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty());
  }

  public TeamPreferencesDomain withCurrency(final Currency newCurrency) {
    Objects.requireNonNull(newCurrency, "Currency cannot be null");
    return internalBuilder()
        .teamId(this.teamId)
        .defaultCurrency(newCurrency)
        .savingsGoal(this.savingsGoal)
        .metadata(this.metadata)
        .lastModifiedBy(this.lastModifiedBy)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public TeamPreferencesDomain withSavingsGoal(final BigDecimal newSavingsGoal) {
    validateSavingsGoal(newSavingsGoal);
    return internalBuilder()
        .teamId(this.teamId)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(newSavingsGoal)
        .metadata(this.metadata)
        .lastModifiedBy(this.lastModifiedBy)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private static void validateSavingsGoal(final BigDecimal savingsGoal) {
    if (savingsGoal != null && isNegative(savingsGoal)) {
      throw new IllegalArgumentException("Savings goal cannot be negative");
    }
  }

  public TeamPreferencesDomain withMetadata(final PreferencesMetadata newMetadata) {
    return internalBuilder()
        .teamId(this.teamId)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(this.savingsGoal)
        .metadata(newMetadata != null ? newMetadata : PreferencesMetadata.empty())
        .lastModifiedBy(this.lastModifiedBy)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public TeamPreferencesDomain withLastModifiedBy(final UUID newLastModifiedBy) {
    return internalBuilder()
        .teamId(this.teamId)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(this.savingsGoal)
        .metadata(this.metadata)
        .lastModifiedBy(newLastModifiedBy)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
