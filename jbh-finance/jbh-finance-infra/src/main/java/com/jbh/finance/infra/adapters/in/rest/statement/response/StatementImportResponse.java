package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.StatementImportResultDTO;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record StatementImportResponse(
    UUID importId,
    String status,
    String errorCode,
    Instant startedAt,
    Instant finishedAt,
    StatementImportSummaryResponse summary,
    List<StatementImportMovementResponse> movements) {

  public static StatementImportResponse fromDTO(final StatementImportResultDTO dto) {
    return new StatementImportResponse(
        dto.importId(),
        dto.status().name(),
        dto.errorCode(),
        dto.startedAt(),
        dto.finishedAt(),
        StatementImportSummaryResponse.fromDTO(dto.summary()),
        dto.movements().stream().map(StatementImportMovementResponse::fromDTO).toList());
  }
}
