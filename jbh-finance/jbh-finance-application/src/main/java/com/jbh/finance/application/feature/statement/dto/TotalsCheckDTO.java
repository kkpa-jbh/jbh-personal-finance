package com.jbh.finance.application.feature.statement.dto;

import java.math.BigDecimal;

/**
 * Totals check of a statement.
 *
 * @param status the check result
 * @param expectedChange closing minus opening balance, or {@code null} when not available
 * @param movementsChange credits minus debits
 */
public record TotalsCheckDTO(
    TotalsCheckStatus status, BigDecimal expectedChange, BigDecimal movementsChange) {}
