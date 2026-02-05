package com.jbh.preferences.application.core.dto;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder(builderMethodName = "internalBuilder")
public record TeamPreferencesDTO(
    UUID teamId,
    Currency defaultCurrency,
    BigDecimal savingsGoal,
    PreferencesMetadata metadata,
    UUID lastModifiedBy,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static TeamPreferencesDTOBuilder defaultBuilder(final UUID teamId) {
    return internalBuilder()
        .teamId(teamId)
        .defaultCurrency(Currency.COP)
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty())
        .lastModifiedBy(null);
  }

  public static TeamPreferencesDTOBuilder builder() {
    return internalBuilder()
        .defaultCurrency(Currency.defaultCurrency())
        .savingsGoal(JBH_ZERO)
        .metadata(PreferencesMetadata.empty())
        .lastModifiedBy(null);
  }
}
