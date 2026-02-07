package com.jbh.products.application.core.comparator;

import com.jbh.products.domain.monthlybalance.MonthlyBalanceDomain;
import java.util.Comparator;
import java.util.UUID;

/** Collection of business-relevant sorting strategies */
public final class AccountMonthlyBalanceComparators {

  /** Natural ordering by period (ascending) - equivalent to your original Comparable */
  public static final Comparator<MonthlyBalanceDomain> BY_PERIOD_ASC =
      Comparator.comparing(MonthlyBalanceDomain::getPeriod);

  /** Reverse chronological order (most recent first) */
  public static final Comparator<MonthlyBalanceDomain> BY_PERIOD_DESC = BY_PERIOD_ASC.reversed();

  /** Sort by balance amount (highest first) */
  public static final Comparator<MonthlyBalanceDomain> BY_CLOSING_BALANCE_DESC =
      Comparator.comparing(MonthlyBalanceDomain::getClosingBalance, Comparator.reverseOrder());

  public static final Comparator<MonthlyBalanceDomain> BY_ACCOUNT_UUID_THEN_PERIOD =
      Comparator.comparing(AccountMonthlyBalanceComparators::extractAccountUuid)
          .thenComparing(MonthlyBalanceDomain::getPeriod);

  private AccountMonthlyBalanceComparators() {
    // Utility class
  }

  private static UUID extractAccountUuid(final MonthlyBalanceDomain balance) {
    return balance.getAccountId().value();
  }
}
