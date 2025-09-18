package com.jbh.account.application.accounts.comparator;

import com.jbh.account.domain.entity.AccountMonthlyBalanceDomain;
import java.util.Comparator;
import java.util.UUID;

/** Collection of business-relevant sorting strategies */
public final class AccountMonthlyBalanceComparators {

  /** Natural ordering by period (ascending) - equivalent to your original Comparable */
  public static final Comparator<AccountMonthlyBalanceDomain> BY_PERIOD_ASC =
      Comparator.comparing(AccountMonthlyBalanceDomain::getPeriod);

  /** Reverse chronological order (most recent first) */
  public static final Comparator<AccountMonthlyBalanceDomain> BY_PERIOD_DESC =
      BY_PERIOD_ASC.reversed();

  /** Sort by balance amount (highest first) */
  public static final Comparator<AccountMonthlyBalanceDomain> BY_CLOSING_BALANCE_DESC =
      Comparator.comparing(
          AccountMonthlyBalanceDomain::getClosingBalance, Comparator.reverseOrder());

  public static final Comparator<AccountMonthlyBalanceDomain> BY_ACCOUNT_UUID_THEN_PERIOD =
      Comparator.comparing(AccountMonthlyBalanceComparators::extractAccountUuid)
          .thenComparing(AccountMonthlyBalanceDomain::getPeriod);

  private AccountMonthlyBalanceComparators() {
    // Utility class
  }

  private static UUID extractAccountUuid(final AccountMonthlyBalanceDomain balance) {
    return balance.getAccountId().value();
  }
}
