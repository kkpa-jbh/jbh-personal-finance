package com.jbh.account.application.accounts.usecases.integration.monthlybalance;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.account.application.accounts.dto.AccountMonthlyBalanceDTO;

public class MonthlyBalanceITUtils {

  public static void assertBalance(
      final AccountMonthlyBalanceDTO expected, final AccountMonthlyBalanceDTO actual) {
    assertBalance(expected, actual, null);
  }

  public static void assertBalance(
      final AccountMonthlyBalanceDTO expected,
      final AccountMonthlyBalanceDTO actual,
      final IgnoreOption... ignoreOptions) {

    boolean ignoreMonthlyProfit = false;
    boolean ignoreOpeningBalance = false;

    // Process provided ignore options
    if (ignoreOptions != null) {
      for (final IgnoreOption option : ignoreOptions) {
        switch (option) {
          case IGNORE_MONTHLY_PROFIT -> ignoreMonthlyProfit = true;
          case IGNORE_OPENING_BALANCE -> ignoreOpeningBalance = true;
        }
      }
    }

    assertEquals(expected.period(), actual.period(), "Period");
    assertEquals(expected.period().getYear(), actual.year(), "Year");
    assertEquals(expected.period().getMonthValue(), actual.month(), "Month");

    assertEquals(expected.closingBalance(), actual.closingBalance(), "Closing Balance");
    assertEquals(expected.movementBalance(), actual.movementBalance(), "Movement Balance");
    assertEquals(expected.totalDebits(), actual.totalDebits(), "Total Debits");
    assertEquals(expected.totalCredits(), actual.totalCredits(), "Total Credits");

    assertEquals(expected.monthlyExpenses(), actual.monthlyExpenses(), "Monthly Expenses");
    assertEquals(expected.totalMovements(), actual.totalMovements(), "Total Movements");
    assertEquals(
        Boolean.valueOf(expected.gapPeriod()), Boolean.valueOf(actual.gapPeriod()), "Gap Period");
    assertEquals(
        Boolean.valueOf(expected.officialMonthlyReport()),
        Boolean.valueOf(actual.officialMonthlyReport()),
        "Official Monthly Report");

    // Async attributes
    if (!ignoreOpeningBalance) {
      assertEquals(
          expected.openingBalance(),
          actual.openingBalance(),
          "Opening Balance for period " + actual.period());
    }
    if (!ignoreMonthlyProfit) {
      assertEquals(
          expected.monthlyProfit(),
          actual.monthlyProfit(),
          "Monthly Profit for period " + actual.period());
    }
  }
}
