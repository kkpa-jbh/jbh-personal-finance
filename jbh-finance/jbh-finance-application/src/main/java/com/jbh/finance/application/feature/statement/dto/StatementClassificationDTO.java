package com.jbh.finance.application.feature.statement.dto;

import java.util.List;

/**
 * Result of a statement import.
 *
 * @param movements categorized movements in the order of the PDF
 * @param totalsCheck totals check of the statement
 */
public record StatementClassificationDTO(
    List<CategorizedMovementDTO> movements, TotalsCheckDTO totalsCheck) {}
