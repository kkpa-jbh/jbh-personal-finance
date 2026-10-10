package com.jbh.finance.application.feature.statement.services;

import com.jbh.finance.application.feature.statement.dto.ExtractedMovementDTO;
import com.jbh.finance.application.feature.statement.dto.ExtractedStatementDTO;
import com.jbh.finance.application.feature.statement.dto.StatementBalanceDTO;
import com.jbh.finance.application.feature.statement.dto.StatementMovementDirection;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckDTO;
import com.jbh.finance.application.feature.statement.dto.TotalsCheckStatus;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Compares the movements of a statement with the balance change printed on it, once per currency.
 */
public final class StatementTotalsChecker {

  private static final int EQUAL = 0;

  /**
   * Checks, for each currency, that credits minus debits equal the closing minus the opening
   * balance.
   *
   * <p>Both signs match: on a credit card statement the balance is a debt, so a purchase (DEBIT)
   * makes the balance grow.
   *
   * @param statement the extracted statement
   * @return one check per currency, in the order the currencies first appear (movements, then
   *     balances)
   */
  public List<TotalsCheckDTO> check(final ExtractedStatementDTO statement) {
    final Set<String> currencies = new LinkedHashSet<>();
    statement.movements().forEach(movement -> currencies.add(movement.currency()));
    statement.balances().forEach(balance -> currencies.add(balance.currency()));

    final List<TotalsCheckDTO> checks = new ArrayList<>(currencies.size());
    for (final String currency : currencies) {
      checks.add(check(statement, currency));
    }
    return checks;
  }

  private static TotalsCheckDTO check(
      final ExtractedStatementDTO statement, final String currency) {
    final BigDecimal movementsChange = movementsChange(statement, currency);
    final Optional<StatementBalanceDTO> balance =
        statement.balances().stream()
            .filter(candidate -> Objects.equals(candidate.currency(), currency))
            .findFirst();

    if (balance.isEmpty()
        || balance.get().openingBalance() == null
        || balance.get().closingBalance() == null) {
      return new TotalsCheckDTO(currency, TotalsCheckStatus.NOT_AVAILABLE, null, movementsChange);
    }

    final BigDecimal expectedChange =
        balance.get().closingBalance().subtract(balance.get().openingBalance());
    final boolean matches =
        movementsChange.compareTo(expectedChange) == EQUAL
            || movementsChange.negate().compareTo(expectedChange) == EQUAL;
    final TotalsCheckStatus status = matches ? TotalsCheckStatus.MATCH : TotalsCheckStatus.MISMATCH;

    return new TotalsCheckDTO(currency, status, expectedChange, movementsChange);
  }

  private static BigDecimal movementsChange(
      final ExtractedStatementDTO statement, final String currency) {
    BigDecimal change = BigDecimal.ZERO;
    for (final ExtractedMovementDTO movement : statement.movements()) {
      if (!Objects.equals(movement.currency(), currency)) {
        continue;
      }
      change =
          movement.direction() == StatementMovementDirection.CREDIT
              ? change.add(movement.amount())
              : change.subtract(movement.amount());
    }
    return change;
  }
}
