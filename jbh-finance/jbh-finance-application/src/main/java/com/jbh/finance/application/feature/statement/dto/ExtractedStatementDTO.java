package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Movements and balances read from a statement PDF.
 *
 * @param movements movements in the order of the PDF
 * @param openingBalance balance at the start of the period, or {@code null} when not shown
 * @param closingBalance balance at the end of the period, or {@code null} when not shown
 */
public record ExtractedStatementDTO(
    List<ExtractedMovementDTO> movements, BigDecimal openingBalance, BigDecimal closingBalance) {

  public ExtractedStatementDTO {
    movements = List.copyOf(movements);
  }
}
