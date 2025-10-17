package com.jbh.account.domain.calculators;

import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MoneyGrowthCalculator {
  public static final BigDecimal HALF_RATE = BigDecimal.valueOf(0.5);
  // Recommended: define a MathContext for precision & rounding
  private static final MathContext MC = new MathContext(12, RoundingMode.HALF_UP);
  private static final Logger log = LoggerFactory.getLogger(MoneyGrowthCalculator.class);

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

    if (denominator.compareTo(BigDecimal.ZERO) <= 0) {
      return withJBHDecimals(BigDecimal.ZERO);
    }

    final BigDecimal growthDec = numerator.divide(denominator, MC);

    final BigDecimal growthRate = withJBHDecimals(growthDec.multiply(BigDecimal.valueOf(100)));

    log.debug(
        "Opening {} Closing {} Movement {} = Growth {}",
        openingBalance,
        closingBalance,
        movementBalance,
        growthRate);
    return growthRate;
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
      log.error("Invalid denominator: opening balance + half flows <= 0");
    }
    return denominator;
  }
}
