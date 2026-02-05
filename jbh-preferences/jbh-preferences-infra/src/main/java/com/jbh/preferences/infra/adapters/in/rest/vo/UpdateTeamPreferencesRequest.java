package com.jbh.preferences.infra.adapters.in.rest.vo;

import com.jbh.preferences.application.core.vo.commands.UpdateTeamPreferencesCommand;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdateTeamPreferencesRequest(
    String defaultCurrency, BigDecimal savingsGoal, Map<String, Object> metadata) {

  public UpdateTeamPreferencesCommand toCommand(final UUID lastModifiedBy) {
    return UpdateTeamPreferencesCommand.builder()
        .defaultCurrency(
            defaultCurrency != null
                ? Currency.fromCode(defaultCurrency)
                : Currency.defaultCurrency())
        .savingsGoal(savingsGoal)
        .metadata(
            metadata != null ? PreferencesMetadata.fromMap(metadata) : PreferencesMetadata.empty())
        .lastModifiedBy(lastModifiedBy)
        .build();
  }
}
