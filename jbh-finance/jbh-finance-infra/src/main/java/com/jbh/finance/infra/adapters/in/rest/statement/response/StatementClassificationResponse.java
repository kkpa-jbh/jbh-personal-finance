package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.StatementClassificationDTO;
import java.util.List;

public record StatementClassificationResponse(
    List<CategorizedMovementResponse> movements, List<TotalsCheckResponse> totalsChecks) {

  public static StatementClassificationResponse fromDTO(final StatementClassificationDTO dto) {
    return new StatementClassificationResponse(
        dto.movements().stream().map(CategorizedMovementResponse::fromDTO).toList(),
        dto.totalsChecks().stream().map(TotalsCheckResponse::fromDTO).toList());
  }
}
