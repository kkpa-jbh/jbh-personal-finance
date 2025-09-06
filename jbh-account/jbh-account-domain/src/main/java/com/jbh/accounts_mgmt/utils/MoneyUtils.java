package com.jbh.accounts_mgmt.utils;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class MoneyUtils {

  public static final BigDecimal JBH_ZERO = withJBHDecimals(BigDecimal.ZERO);

  public static BigDecimal withJBHDecimals(BigDecimal amount) {
    if (amount == null) {
      return null;
    }
    return amount.setScale(2, RoundingMode.UNNECESSARY);
  }

}
