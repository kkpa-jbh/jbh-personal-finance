package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A statement movement that was not saved.
 *
 * @param date posting date
 * @param description movement text
 * @param amount always positive
 * @param currency ISO 4217 code
 * @param direction money in or money out
 * @param reason why it was not saved
 * @param categoryAlias the suggested category alias
 * @param categoryDisplayName the category name per language
 * @param confidence classifier confidence (0 to 1)
 * @param needsReview {@code true} when the user should check the category
 */
public record SkippedStatementMovementDTO(
    LocalDate date,
    String description,
    BigDecimal amount,
    String currency,
    StatementMovementDirection direction,
    SkipReason reason,
    String categoryAlias,
    Map<String, String> categoryDisplayName,
    BigDecimal confidence,
    boolean needsReview) {

  /** Why a movement was not saved. */
  public enum SkipReason {
    OTHER_CURRENCY,
    FUTURE_DATE
  }
}
