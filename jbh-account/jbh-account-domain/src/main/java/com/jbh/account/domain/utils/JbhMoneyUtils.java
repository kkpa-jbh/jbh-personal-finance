package com.jbh.account.domain.utils;

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

  public static BigDecimal divide(final BigDecimal amount, final BigDecimal divisor) {
    return amount.divide(divisor, 2, RoundingMode.HALF_EVEN);
  }

  public static boolean isNegative(final BigDecimal totalAmount) {
    return totalAmount != null && totalAmount.signum() < 0;
  }

  public static BigDecimal toDecimal(final Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof BigDecimal) {
      return (BigDecimal) value;
    }
    return new BigDecimal(value.toString());
  }
}
