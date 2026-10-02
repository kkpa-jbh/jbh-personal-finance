package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One movement read from a statement PDF.
 *
 * @param date posting date
 * @param description movement text as printed on the statement
 * @param amount always positive; {@code direction} gives the sign
 * @param currency ISO 4217 code, for example {@code COP}
 * @param direction money in or money out
 */
public record ExtractedMovementDTO(
    LocalDate date,
    String description,
    BigDecimal amount,
    String currency,
    StatementMovementDirection direction) {}
