package com.jbh.finance.application.feature.statement.dto;

import java.util.List;

/**
 * Summary of a finished statement import.
 *
 * @param mainCurrency the currency whose movements were saved
 * @param savedCount movements saved
 * @param skippedCount movements not saved
 * @param needsReviewCount saved movements the user should check
 * @param totalsChecks one totals check per currency
 * @param notSavedMovements the movements that were not saved
 */
public record StatementImportSummaryDTO(
    String mainCurrency,
    int savedCount,
    int skippedCount,
    int needsReviewCount,
    List<TotalsCheckDTO> totalsChecks,
    List<SkippedStatementMovementDTO> notSavedMovements) {}
