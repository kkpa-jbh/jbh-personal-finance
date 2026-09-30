package com.jbh.finance.infra.adapters.in.rest.statement.response;

import com.jbh.finance.application.feature.statement.dto.CategorizedMovementDTO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

public record CategorizedMovementResponse(
    LocalDate date,
    String description,
    BigDecimal amount,
    String direction,
    String categoryAlias,
    Map<String, String> categoryDisplayName,
    BigDecimal confidence,
    boolean lowConfidence) {

  public static CategorizedMovementResponse fromDTO(final CategorizedMovementDTO dto) {
    return new CategorizedMovementResponse(
        dto.date(),
        dto.description(),
        dto.amount(),
        dto.direction().name(),
        dto.categoryAlias(),
        dto.categoryDisplayName(),
        dto.confidence(),
        dto.lowConfidence());
  }
}
