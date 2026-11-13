package com.jbh.finance.application.feature.statement.services;

import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Picks the main currency of a statement: the currency with the most movements. On a tie, the
 * currency whose balance section comes first in the PDF; if none of the tied currencies has a
 * balance section, the one that appears first in the movements.
 */
public final class StatementMainCurrencyPolicy {

  /**
   * Picks the main currency.
   *
   * @param movements the statement movements, not empty
   * @param balances the balance sections in PDF order, may be empty
   * @return the ISO 4217 code of the main currency
   * @throws IllegalArgumentException when there are no movements
   */
  public String pick(
      final List<CategorizedMovementDTO> movements, final List<StatementBalanceDTO> balances) {
    if (movements == null || movements.isEmpty()) {
      throw new IllegalArgumentException("A statement needs at least one movement");
    }
    final Map<String, Long> countByCurrency = new LinkedHashMap<>();
    for (final CategorizedMovementDTO movement : movements) {
      countByCurrency.merge(movement.currency(), 1L, Long::sum);
    }
    final long highest = Collections.max(countByCurrency.values());
    final List<String> tied =
        countByCurrency.entrySet().stream()
            .filter(entry -> entry.getValue() == highest)
            .map(Map.Entry::getKey)
            .toList();

    return balances.stream()
        .map(StatementBalanceDTO::currency)
        .filter(tied::contains)
        .findFirst()
        .orElse(tied.get(0));
  }
}
