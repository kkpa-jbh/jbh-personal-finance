package com.jbh.preferences.application.core.dto;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesId;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(builderMethodName = "internalBuilder")
public record UserPreferencesDTO(
    PreferencesId id,
    UUID userId,
    Language defaultLang,
    Currency defaultCurrency,
    BigDecimal savingsGoal,
    UUID defaultAccountId,
    PreferencesMetadata metadata,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static UserPreferencesDTOBuilder defaultBuilder(final UUID userId) {
    return internalBuilder()
        .userId(userId)
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty());
  }

  public static UserPreferencesDTOBuilder builder() {
    return internalBuilder()
        .defaultLang(Language.DEFAULT)
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty());
  }
}
