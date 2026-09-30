package com.jbh.finance.application.feature.statement.services;

import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckStatus;
import java.math.BigDecimal;

/** Compares the movements of a statement with the balance change printed on it. */
public final class StatementTotalsChecker {

  private static final int EQUAL = 0;

  /**
   * Checks that credits minus debits equal the closing minus the opening balance.
   *
   * <p>Both signs match: on a credit card statement the balance is a debt, so a purchase (DEBIT)
   * makes the balance grow.
   *
   * @param statement the extracted statement
   * @return the check result
   */
  public TotalsCheckDTO check(final ExtractedStatementDTO statement) {
    final BigDecimal movementsChange = movementsChange(statement);

    if (statement.openingBalance() == null || statement.closingBalance() == null) {
      return new TotalsCheckDTO(TotalsCheckStatus.NOT_AVAILABLE, null, movementsChange);
    }

    final BigDecimal expectedChange =
        statement.closingBalance().subtract(statement.openingBalance());
    final boolean matches =
        movementsChange.compareTo(expectedChange) == EQUAL
            || movementsChange.negate().compareTo(expectedChange) == EQUAL;
    final TotalsCheckStatus status = matches ? TotalsCheckStatus.MATCH : TotalsCheckStatus.MISMATCH;

    return new TotalsCheckDTO(status, expectedChange, movementsChange);
  }

  private static BigDecimal movementsChange(final ExtractedStatementDTO statement) {
    BigDecimal change = BigDecimal.ZERO;
    for (final ExtractedMovementDTO movement : statement.movements()) {
      change =
          movement.direction() == StatementMovementDirection.CREDIT
              ? change.add(movement.amount())
              : change.subtract(movement.amount());
    }
    return change;
  }
}
