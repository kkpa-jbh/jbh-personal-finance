package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import java.math.BigDecimal;

public record TotalsCheckResponse(
    String currency,
    String status,
    BigDecimal expectedBalanceChange,
    BigDecimal movementsBalanceChange) {

  public static TotalsCheckResponse fromDTO(final TotalsCheckDTO dto) {
    return new TotalsCheckResponse(
        dto.currency(), dto.status().name(), dto.expectedChange(), dto.movementsChange());
  }
}
