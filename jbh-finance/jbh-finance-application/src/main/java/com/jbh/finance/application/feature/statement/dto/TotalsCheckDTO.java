package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;

/**
 * Totals check of one currency of a statement.
 *
 * @param currency ISO 4217 code, for example {@code COP}
 * @param status the check result
 * @param expectedChange closing minus opening balance, or {@code null} when not available
 * @param movementsChange credits minus debits of the movements in this currency
 */
public record TotalsCheckDTO(
    String currency,
    TotalsCheckStatus status,
    BigDecimal expectedChange,
    BigDecimal movementsChange) {}
