package com.jbh.account.domain.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {

  public static final BigDecimal JBH_ZERO = withJBHDecimals(BigDecimal.ZERO);

  private MoneyUtils() {
    // Utility class
  }

  public static BigDecimal withJBHDecimals(final BigDecimal amount) {
    if (amount == null) {
      return null;
    }
    return amount.setScale(2, RoundingMode.UNNECESSARY);
  }

  public static boolean isZero(final BigDecimal amount) {
    if (amount == null) {
      return true;
    }

    return amount.equals(BigDecimal.ZERO) || withJBHDecimals(amount).equals(JBH_ZERO);
  }

  public static boolean isNotZero(final BigDecimal amount) {
    return !isZero(amount);
  }
}
