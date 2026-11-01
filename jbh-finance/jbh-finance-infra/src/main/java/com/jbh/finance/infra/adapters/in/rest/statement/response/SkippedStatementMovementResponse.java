package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.SkippedStatementMovementDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record SkippedStatementMovementResponse(
    LocalDate date,
    String description,
    BigDecimal amount,
    String currency,
    String direction,
    String reason,
    String categoryAlias,
    Map<String, String> categoryDisplayName,
    BigDecimal confidence,
    boolean needsReview) {

  public static SkippedStatementMovementResponse fromDTO(final SkippedStatementMovementDTO dto) {
    return new SkippedStatementMovementResponse(
        dto.date(),
        dto.description(),
        dto.amount(),
        dto.currency(),
        dto.direction().name(),
        dto.reason().name(),
        dto.categoryAlias(),
        dto.categoryDisplayName(),
        dto.confidence(),
        dto.needsReview());
  }
}
