package com.jbh.account.domain.calculators;

import com.jbh.account.domain.utils.MoneyUtils;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MoneyGrowthCalculator {

  public static final BigDecimal HALF_RATE = BigDecimal.valueOf(0.5);
  // Recommended: define a MathContext for precision & rounding
  private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);

  /**
   * Calculates the monthly growth percentage (as a decimal, e.g. 0.025 = 2.5%)
   *
   * @param openingBalance balance at the beginning of the month
   * @param closingBalance balance at the end of the month
   * @param movementBalance debits - credits during the month (positive for deposits, negative for
   *     withdrawals)
   * @return monthly growth as BigDecimal (decimal, not %)
   */
  public BigDecimal calculateMonthlyGrowth(
      final BigDecimal openingBalance,
      final BigDecimal closingBalance,
      final BigDecimal movementBalance) {
    final var denominator = getDenominator(openingBalance, closingBalance, movementBalance);

    // numerator = closingBalance - openingBalance - movementBalance
    final BigDecimal numerator =
        closingBalance.subtract(openingBalance, MC).subtract(movementBalance, MC);

    final BigDecimal growthDec = numerator.divide(denominator, MC);

    return MoneyUtils.withJBHDecimals(growthDec.multiply(BigDecimal.valueOf(100)));
  }

  private BigDecimal getDenominator(
      final BigDecimal openingBalance,
      final BigDecimal closingBalance,
      final BigDecimal movementBalance) {
    if (openingBalance == null || closingBalance == null || movementBalance == null) {
      throw new IllegalArgumentException("Balances cannot be null");
    }

    // denominator = openingBalance + 0.5 * movementBalance
    final BigDecimal halfFlows = movementBalance.multiply(HALF_RATE, MC);
    final BigDecimal denominator = openingBalance.add(halfFlows, MC);

    if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ArithmeticException("Invalid denominator: opening balance + half flows <= 0");
    }
    return denominator;
  }
}
