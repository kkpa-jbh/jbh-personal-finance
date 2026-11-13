package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.StatementImportMovementDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

public record StatementImportMovementResponse(
    UUID movementId,
    LocalDate date,
    String description,
    BigDecimal amount,
    String direction,
    CategoryRefResponse category,
    BigDecimal confidence,
    boolean needsReview) {

  public static StatementImportMovementResponse fromDTO(final StatementImportMovementDTO dto) {
    return new StatementImportMovementResponse(
        dto.movementId(),
        dto.date(),
        dto.description(),
        dto.amount(),
        dto.direction().name(),
        new CategoryRefResponse(dto.categoryAlias(), dto.categoryDisplayName()),
        dto.confidence(),
        dto.needsReview());
  }

  /** The category of a saved movement. */
  public record CategoryRefResponse(String alias, Map<String, String> displayName) {}
}
