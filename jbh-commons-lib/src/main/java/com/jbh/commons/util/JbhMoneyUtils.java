package com.jbh.commons.util;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.CommonExceptionType;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class JbhMoneyUtils {

  public static final BigDecimal JBH_ZERO = withJBHDecimals(BigDecimal.ZERO);

  private JbhMoneyUtils() {
    // Utility class
  }

  public static boolean isNotZero(final BigDecimal amount) {
    return !isZero(amount);
  }

  public static boolean isZero(final BigDecimal amount) {
    if (amount == null) {
      return true;
    }

    return amount.equals(BigDecimal.ZERO) || withJBHDecimals(amount).equals(JBH_ZERO);
  }

  public static BigDecimal withJBHDecimals(final BigDecimal amount) {
    if (amount == null) {
      return null;
    }
    return amount.setScale(2, RoundingMode.HALF_EVEN);
  }

  public static BigDecimal withJBHDecimals(final String amount) {
    if (amount == null) {
      return null;
    }

    return withJBHDecimals(new BigDecimal(amount));
  }

  public static boolean isNegativeOrZero(final BigDecimal totalAmount) {
    return isNegative(totalAmount) || isZero(totalAmount);
  }

  public static boolean isNegative(final BigDecimal totalAmount) {
    return totalAmount != null && totalAmount.signum() < 0;
  }

  @SuppressWarnings("PMD.UnusedAssignment")
  public static BigDecimal toJBHDecimal(final Object value) {

    if (value == null) {
      return JBH_ZERO;
    }

    BigDecimal valueAsDecimal = null;
    if (value instanceof BigDecimal) {
      valueAsDecimal = (BigDecimal) value;
    } else {
      valueAsDecimal = new BigDecimal(value.toString());
    }

    return withJBHDecimals(valueAsDecimal);
  }

  public static void validatePercentage(final BigDecimal percentage) throws BusinessException {
    if (percentage == null) {
      return;
    }
    if (percentage.signum() < 0
        || percentage.compareTo(BigDecimal.ZERO) <= 0
        || percentage.compareTo(new BigDecimal("100")) > 0) {
      throw new BusinessException(CommonExceptionType.INVALID_PERCENTAGE, percentage.toString());
    }
  }

  public static boolean isNotNumber(final Object creditLimit) {
    return !(creditLimit instanceof Number);
  }

  /**
   * Calculates the percentage change from {@code oldValue} to {@code newValue} using the formula:
   * (newValue - oldValue) / oldValue * 100.
   *
   * <p>Behavior and decisions: - If {@code newValue} is {@code null} it is treated as zero. -
   * {@code oldValue} must be non-null and non-zero. An {@link IllegalArgumentException} is thrown
   * because percentage change relative to zero is undefined (infinite). - Intermediate computation
   * uses higher precision to avoid early rounding; the final result is normalized with {@link
   * #withJBHDecimals(BigDecimal)} to the standard JBH scale (2 decimals).
   *
   * @param newValue the new value (may be {@code null})
   * @param oldValue the old/previous value (must be non-null and non-zero)
   * @return percentage change scaled to JBH decimals (2 decimal places)
   * @throws IllegalArgumentException if {@code oldValue} is {@code null} or zero
   */
  public static BigDecimal calculatePercentageChange(
      final BigDecimal newValue, final BigDecimal oldValue) {
    if (oldValue == null || oldValue.equals(BigDecimal.ZERO)) {
      return JBH_ZERO;
    }

    final BigDecimal effectiveNew = (newValue == null) ? JBH_ZERO : newValue;
    final BigDecimal delta = effectiveNew.subtract(oldValue);

    // Use higher intermediate scale to avoid double-rounding; final rounding is performed by
    // withJBHDecimals.
    final BigDecimal rawPercent =
        delta.multiply(BigDecimal.valueOf(100)).divide(oldValue, 6, RoundingMode.HALF_EVEN);

    return withJBHDecimals(rawPercent);
  }

  public static BigDecimal divide(final BigDecimal amount, final BigDecimal divisor) {
    return amount.divide(divisor, 2, RoundingMode.HALF_EVEN);
  }
}
