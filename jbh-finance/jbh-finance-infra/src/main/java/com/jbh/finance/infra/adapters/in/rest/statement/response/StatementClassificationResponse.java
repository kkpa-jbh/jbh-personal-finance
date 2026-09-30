package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import java.math.BigDecimal;
import java.util.List;

public record StatementClassificationResponse(
    List<CategorizedMovementResponse> movements,
    String totalsCheckStatus,
    BigDecimal expectedBalanceChange,
    BigDecimal movementsBalanceChange) {

  public static StatementClassificationResponse fromDTO(final StatementClassificationDTO dto) {
    final TotalsCheckDTO totals = dto.totalsCheck();
    return new StatementClassificationResponse(
        dto.movements().stream().map(CategorizedMovementResponse::fromDTO).toList(),
        totals.status().name(),
        totals.expectedChange(),
        totals.movementsChange());
  }
}
