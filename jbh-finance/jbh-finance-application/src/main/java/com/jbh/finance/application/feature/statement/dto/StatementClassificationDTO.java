package com.jbh.finance.application.feature.statement.dto;

import java.util.List;

/**
 * Result of a statement import.
 *
 * @param movements categorized movements in the order of the PDF
 * @param totalsChecks one totals check per currency
 * @param balances the balances of each currency section of the statement; empty when none
 */
public record StatementClassificationDTO(
    List<CategorizedMovementDTO> movements,
    List<TotalsCheckDTO> totalsChecks,
    List<StatementBalanceDTO> balances) {}
