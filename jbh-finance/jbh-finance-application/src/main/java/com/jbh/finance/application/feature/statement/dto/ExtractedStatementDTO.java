package com.jbh.finance.application.feature.statement.dto;

import java.util.List;

/**
 * Movements and balances read from a statement PDF.
 *
 * @param movements movements in the order of the PDF
 * @param balances the balances of each currency section; empty when none is shown
 */
public record ExtractedStatementDTO(
    List<ExtractedMovementDTO> movements, List<StatementBalanceDTO> balances) {

  public ExtractedStatementDTO {
    movements = List.copyOf(movements);
    balances = List.copyOf(balances);
  }
}
