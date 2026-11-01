package com.jbh.finance.application.feature.statement.services;

import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
import java.util.List;

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
    throw new UnsupportedOperationException("not implemented");
  }
}
