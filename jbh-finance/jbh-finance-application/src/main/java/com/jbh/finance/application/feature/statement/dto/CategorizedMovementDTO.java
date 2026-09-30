package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

/**
 * A statement movement with its suggested category.
 *
 * @param date posting date
 * @param description movement text
 * @param amount always positive
 * @param direction money in or money out
 * @param categoryAlias suggested category alias
 * @param categoryDisplayName category name per language
 * @param confidence classifier confidence (0 to 1)
 * @param lowConfidence {@code true} when the user should check the category
 */
public record CategorizedMovementDTO(
    LocalDate date,
    String description,
    BigDecimal amount,
    StatementMovementDirection direction,
    String categoryAlias,
    Map<String, String> categoryDisplayName,
    BigDecimal confidence,
    boolean lowConfidence) {}
