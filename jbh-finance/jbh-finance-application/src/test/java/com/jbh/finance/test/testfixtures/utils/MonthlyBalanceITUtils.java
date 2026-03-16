package com.jbh.finance.test.testfixtures.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jbh.finance.application.feature.monthlybalance.commands.AddMonthlyBalanceCommand;
import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.test.testfixtures.builders.commands.MonthlyBalanceCommandFixture;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;

public class MonthlyBalanceITUtils {

  public static void assertMonthlyBalance(
      final MonthlyBalanceDTO expected, final MonthlyBalanceDTO actual) {
    assertMonthlyBalance(expected, actual, null);
  }

  public static void assertMonthlyBalance(
      final MonthlyBalanceDTO expected,
      final MonthlyBalanceDTO actual,
      final MonthlyBalanceIgnoreOption... ignoreOptions) {

    boolean ignoreMonthlyProfit = false;
    boolean ignoreOpeningBalance = false;
    boolean ignoreNetGrowthRate = false;
    boolean ignoreClosingBalance = false;

    // Process provided ignore options
    if (ignoreOptions != null) {
      for (final MonthlyBalanceIgnoreOption option : ignoreOptions) {
        switch (option) {
          case IGNORE_MONTHLY_PROFIT -> ignoreMonthlyProfit = true;
          case IGNORE_OPENING_BALANCE -> ignoreOpeningBalance = true;
          case IGNORE_NET_GROWTH_RATE -> ignoreNetGrowthRate = true;
          case IGNORE_CLOSING_BALANCE -> ignoreClosingBalance = true;
        }
      }
    }

    assertEquals(expected.period(), actual.period(), "Period");
    assertEquals(expected.period().getYear(), actual.year(), "Year");
    assertEquals(expected.period().getMonthValue(), actual.month(), "Month");

    if (!ignoreClosingBalance) {
      assertEquals(
          expected.closingBalance(),
          actual.closingBalance(),
          "Closing Balance for period " + actual.period());
    }

    assertEquals(
        expected.movementBalance(),
        actual.movementBalance(),
        "Movement Balance " + actual.period());
    assertEquals(
        expected.totalDebits(), actual.totalDebits(), "Total Debits for period " + actual.period());
    assertEquals(
        expected.totalCredits(),
        actual.totalCredits(),
        "Total Credits for period " + actual.period());

    assertEquals(
        expected.totalMovements(),
        actual.totalMovements(),
        "Total Movements for period " + actual.period());
    assertEquals(
        Boolean.valueOf(expected.gapPeriod()),
        Boolean.valueOf(actual.gapPeriod()),
        "Gap Period for period " + actual.period());
    assertEquals(
        Boolean.valueOf(expected.officialMonthlyReport()),
        Boolean.valueOf(actual.officialMonthlyReport()),
        "Official Monthly Report for period " + actual.period());

    // Async attributes
    if (!ignoreOpeningBalance) {
      assertEquals(
          expected.openingBalance(),
          actual.openingBalance(),
          "Opening Balance for period " + actual.period());
    }
    if (!ignoreMonthlyProfit) {
      assertEquals(
          expected.monthlyNetProfit(),
          actual.monthlyNetProfit(),
          "Monthly Net Profit for period " + actual.period());
    }
  }

  public static AddMonthlyBalanceCommand createMonthlyBalanceCommand(
      final YearMonth monthlyPeriod, final MonthlyBalanceCommandFixture balanceVO) {
    if (balanceVO == null) {
      return new AddMonthlyBalanceCommand(monthlyPeriod, null, null, null);
    }
    return new AddMonthlyBalanceCommand(
        monthlyPeriod, balanceVO.closingBalance(), balanceVO.monthlyProfitReported(), null);
  }

  public static AddMonthlyBalanceCommand createMonthlyBalanceCommand(
      final YearMonth monthlyPeriod,
      final BigDecimal closingBalance,
      final BigDecimal monthlyProfitReported) {
    return new AddMonthlyBalanceCommand(monthlyPeriod, closingBalance, monthlyProfitReported, null);
  }

  public static AddMonthlyBalanceCommand createMonthlyBalanceCommand(
      final YearMonth monthlyPeriod,
      final MonthlyBalanceCommandFixture balanceVO,
      final BigDecimal retefuente) {
    return new AddMonthlyBalanceCommand(
        monthlyPeriod, balanceVO.closingBalance(), balanceVO.monthlyProfitReported(), retefuente);
  }

  public static MonthlyBalanceDTO getBalanceForPeriod(
      final List<MonthlyBalanceDTO> monthlyBalanceBefore, final YearMonth initialDepositDate) {
    return monthlyBalanceBefore.stream()
        .filter(b -> b.period().equals(initialDepositDate))
        .findFirst()
        .orElseThrow(
            () ->
                new RuntimeException("No monthly balance found for period " + initialDepositDate));
  }
}
