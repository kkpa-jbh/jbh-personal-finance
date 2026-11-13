package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.StatementImportSummaryDTO;
import java.util.List;

public record StatementImportSummaryResponse(
    String mainCurrency,
    int savedCount,
    int skippedCount,
    int needsReviewCount,
    List<TotalsCheckResponse> totalsChecks,
    List<SkippedStatementMovementResponse> notSavedMovements) {

  public static StatementImportSummaryResponse fromDTO(final StatementImportSummaryDTO dto) {
    if (dto == null) {
      return null;
    }
    return new StatementImportSummaryResponse(
        dto.mainCurrency(),
        dto.savedCount(),
        dto.skippedCount(),
        dto.needsReviewCount(),
        dto.totalsChecks().stream().map(TotalsCheckResponse::fromDTO).toList(),
        dto.notSavedMovements().stream().map(SkippedStatementMovementResponse::fromDTO).toList());
  }
}
