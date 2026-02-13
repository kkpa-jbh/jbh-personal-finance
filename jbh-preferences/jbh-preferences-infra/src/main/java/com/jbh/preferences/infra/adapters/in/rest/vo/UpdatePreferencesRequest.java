package com.jbh.preferences.infra.adapters.in.rest.vo;

import com.jbh.preferences.application.core.vo.commands.UpdatePreferencesCommand;
import com.jbh.preferences.domain.vo.Currency;
import com.jbh.preferences.domain.vo.Language;
import com.jbh.preferences.domain.vo.PreferencesMetadata;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

public record UpdatePreferencesRequest(
    String defaultLang,
    String defaultCurrency,
    BigDecimal savingsGoal,
    UUID defaultProductId,
    Map<String, Object> metadata) {

  public UpdatePreferencesCommand toCommand() {
    return UpdatePreferencesCommand.builder()
        .defaultLang(defaultLang != null ? Language.of(defaultLang) : Language.DEFAULT)
        .defaultCurrency(
            defaultCurrency != null
                ? Currency.fromCode(defaultCurrency)
                : Currency.defaultCurrency())
        .savingsGoal(savingsGoal)
        .defaultProductId(defaultProductId)
        .metadata(
            metadata != null ? PreferencesMetadata.fromMap(metadata) : PreferencesMetadata.empty())
        .build();
  }
}
