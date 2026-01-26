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

  public static BigDecimal divide(final BigDecimal amount, final BigDecimal divisor) {
    return amount.divide(divisor, 2, RoundingMode.HALF_EVEN);
  }

  public static boolean isNegativeOrZero(final BigDecimal totalAmount) {
    return isNegative(totalAmount) || isZero(totalAmount);
  }

  public static boolean isNegative(final BigDecimal totalAmount) {
    return totalAmount != null && totalAmount.signum() < 0;
  }

  public static BigDecimal toDecimal(final Object value) {
    BigDecimal valueAsDecimal = null;
    if (value == null) {
      return valueAsDecimal;
    }

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
}
