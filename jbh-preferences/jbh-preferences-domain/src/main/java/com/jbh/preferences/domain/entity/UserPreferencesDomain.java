package com.jbh.preferences.domain.entity;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.commons.util.JbhMoneyUtils.isNegative;

import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder(builderMethodName = "internalBuilder")
public final class UserPreferencesDomain {

  private final UUID userId;
  private final Language defaultLang;
  private final Currency defaultCurrency;
  private final BigDecimal savingsGoal;
  private final UUID defaultProductId;
  private final PreferencesMetadata metadata;
  private final LocalDateTime createdAt;
  private final LocalDateTime updatedAt;

  public static UserPreferencesDomain createDefault(final UUID userId) {
    Objects.requireNonNull(userId, "User ID cannot be null");

    return internalBuilder()
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.COP)
        .savingsGoal(JBH_ZERO)
        .defaultProductId(null)
        .metadata(PreferencesMetadata.empty())
        .createdAt(LocalDateTime.now())
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public static UserPreferencesDomainBuilder builder(final UUID userId) {
    Objects.requireNonNull(userId, "User ID cannot be null");

    return internalBuilder()
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty());
  }

  public UserPreferencesDomain withLanguage(final Language newLanguage) {
    Objects.requireNonNull(newLanguage, "Language cannot be null");
    return internalBuilder()
        .userId(this.userId)
        .defaultLang(newLanguage)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(this.savingsGoal)
        .defaultProductId(this.defaultProductId)
        .metadata(this.metadata)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public UserPreferencesDomain withCurrency(final Currency newCurrency) {
    Objects.requireNonNull(newCurrency, "Currency cannot be null");
    return internalBuilder()
        .userId(this.userId)
        .defaultLang(this.defaultLang)
        .defaultCurrency(newCurrency)
        .savingsGoal(this.savingsGoal)
        .defaultProductId(this.defaultProductId)
        .metadata(this.metadata)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public UserPreferencesDomain withSavingsGoal(final BigDecimal newSavingsGoal) {
    validateSavingsGoal(newSavingsGoal);
    return internalBuilder()
        .userId(this.userId)
        .defaultLang(this.defaultLang)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(newSavingsGoal)
        .defaultProductId(this.defaultProductId)
        .metadata(this.metadata)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  private static void validateSavingsGoal(final BigDecimal savingsGoal) {
    if (savingsGoal != null && isNegative(savingsGoal)) {
      throw new IllegalArgumentException("Savings goal cannot be negative");
    }
  }

  public UserPreferencesDomain withDefaultProductId(final UUID newDefaultAccountId) {
    return internalBuilder()
        .userId(this.userId)
        .defaultLang(this.defaultLang)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(this.savingsGoal)
        .defaultProductId(newDefaultAccountId)
        .metadata(this.metadata)
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }

  public UserPreferencesDomain withMetadata(final PreferencesMetadata newMetadata) {
    return internalBuilder()
        .userId(this.userId)
        .defaultLang(this.defaultLang)
        .defaultCurrency(this.defaultCurrency)
        .savingsGoal(this.savingsGoal)
        .defaultProductId(this.defaultProductId)
        .metadata(newMetadata != null ? newMetadata : PreferencesMetadata.empty())
        .createdAt(this.createdAt)
        .updatedAt(LocalDateTime.now())
        .build();
  }
}
