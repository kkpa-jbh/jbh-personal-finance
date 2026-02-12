package com.jbh.finance.domain.product.service.calculators;

import com.jbh.commons.util.JbhMoneyUtils;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Calculator for Money-Weighted Rate of Return (MWRR) using the XIRR method.
 *
 * <p>The Money-Weighted Rate of Return measures the rate of return on an investment portfolio,
 * taking into account the timing and size of cash flows (deposits and withdrawals). This is
 * particularly useful for evaluating investment performance when there are irregular cash flows
 * throughout the investment period.
 *
 * <p>This implementation uses the XIRR (Extended Internal Rate of Return) algorithm, which employs
 * the Newton-Raphson iterative method to find the discount rate that makes the Net Present Value
 * (NPV) of all cash flows equal to zero.
 *
 * @see <a href="https://en.wikipedia.org/wiki/Internal_rate_of_return">Internal Rate of Return</a>
 */
@SuppressWarnings({
  "PMD.CyclomaticComplexity",
  "PMD.AvoidDecimalLiteralsInBigDecimalConstructor",
  "PMD.AvoidInstantiatingObjectsInLoops"
})
public final class MoneyWeightedReturnCalculator {

  /** Convergence threshold for the Newton-Raphson iteration */
  private static final double EPSILON = 1e-7;

  /** Maximum number of iterations to prevent infinite loops */
  private static final int MAX_ITER = 1000;

  private MoneyWeightedReturnCalculator() {}

  /**
   * Calculates the annualized Money-Weighted Rate of Return (MWRR) using the XIRR method.
   *
   * <p>The XIRR algorithm finds the discount rate (r) that satisfies the equation:
   *
   * <pre>
   * NPV = Σ(CF_i / (1 + r)^(t_i)) = 0
   * </pre>
   *
   * where:
   *
   * <ul>
   *   <li>CF_i = cash flow at time i (negative for investments, positive for returns)
   *   <li>t_i = time period in years from the start date to cash flow date i
   *   <li>r = the discount rate (MWRR) we're solving for
   * </ul>
   *
   * <p>The method uses Newton-Raphson iteration:
   *
   * <pre>
   * r_(n+1) = r_n - f(r_n) / f'(r_n)
   * </pre>
   *
   * where f(r) is the NPV function and f'(r) is its derivative.
   *
   * @param cashFlows List of cash flows as BigDecimal values. Negative values represent
   *     investments/deposits, positive values represent returns/withdrawals. Must have the same
   *     size as dates.
   * @param dates List of dates corresponding to each cash flow. Must have the same size as
   *     cashFlows. The first date is used as the reference point (t=0).
   * @return The annualized rate of return as a decimal (e.g., 0.15 means 15% annual return)
   * @throws IllegalArgumentException if cashFlows and dates have different sizes
   * @throws ArithmeticException if the algorithm fails to converge within MAX_ITER iterations
   */
  public static BigDecimal calculateXIRR(
      final List<BigDecimal> cashFlows, final List<LocalDate> dates) {

    if (cashFlows.isEmpty() || dates.isEmpty()) {
      throw new IllegalArgumentException("Cash flows and dates cannot be empty");
    }

    if (cashFlows.size() != dates.size()) {
      throw new IllegalArgumentException("Cash flows and dates must have same size");
    }

    final LocalDate startDate = dates.get(0);
    double guess = 0.10; // Initial guess: 10% annual return

    // Newton-Raphson iteration to find the rate that makes NPV = 0
    for (int i = 0; i < MAX_ITER; i++) {
      double f = 0.0; // NPV at current guess
      double fPrime = 0.0; // Derivative of NPV at current guess

      // Calculate NPV and its derivative for all cash flows
      for (int j = 0; j < cashFlows.size(); j++) {
        final double cashFlow = cashFlows.get(j) != null ? cashFlows.get(j).doubleValue() : 0.0;
        final double days = ChronoUnit.DAYS.between(startDate, dates.get(j));
        final double yearsFromStart = days / 365.0;
        final double discountFactor = Math.pow(1.0 + guess, yearsFromStart);

        // Add discounted cash flow to NPV: CF / (1 + r)^t
        f += cashFlow / discountFactor;

        // Add to derivative: -t * CF / ((1 + r)^(t+1))
        fPrime += -yearsFromStart * cashFlow / (discountFactor * (1.0 + guess));
      }

      // Newton-Raphson update step
      final double newGuess = guess - f / fPrime;

      // Check for convergence
      if (Math.abs(newGuess - guess) <= EPSILON) {
        return JbhMoneyUtils.withJBHDecimals(
            new BigDecimal(newGuess * 100)); // Converged: return annualized IRR
      }

      guess = newGuess;
    }

    // Failed to converge within maximum iterations
    throw new ArithmeticException("XIRR did not converge");
  }

  /**
   * Converts an annual rate of return to a monthly rate of return.
   *
   * <p>Uses the compound interest formula:
   *
   * <pre>
   * monthly_rate = (1 + annual_rate)^(1/12) - 1
   * </pre>
   *
   * @param annualPercentageRate The annual rate as a percentage (e.g., 15%)
   * @return The equivalent monthly rate as a decimal (e.g., 0.0117 for ~1.17% monthly)
   */
  public static BigDecimal toMonthlyRate(final BigDecimal annualPercentageRate) {
    final BigDecimal annualRate = JbhMoneyUtils.divide(annualPercentageRate, new BigDecimal(100));
    final double result = Math.pow(1 + annualRate.doubleValue(), 1.0 / 12) - 1;
    return JbhMoneyUtils.withJBHDecimals(new BigDecimal(result * 100));
  }
}
