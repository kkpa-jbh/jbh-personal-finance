package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/**
 * A movement saved by a statement import.
 *
 * @param movementId the movement id
 * @param date posting date
 * @param description movement text
 * @param amount always positive; {@code direction} gives the sign
 * @param direction money in or money out
 * @param categoryAlias the category alias
 * @param categoryDisplayName the category name per language
 * @param confidence classifier confidence, or {@code null} when not stored
 * @param needsReview {@code true} when the user should check the category
 */
public record StatementImportMovementDTO(
    UUID movementId,
    LocalDate date,
    String description,
    BigDecimal amount,
    StatementMovementDirection direction,
    String categoryAlias,
    Map<String, String> categoryDisplayName,
    BigDecimal confidence,
    boolean needsReview) {}
