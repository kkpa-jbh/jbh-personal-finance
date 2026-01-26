package com.jbh.account.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.commons.util.JbhMoneyUtils;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class MoneyUtilsTest {

  @Test
  public void shouldConvertToJBHDecimals() {
    assertEquals(new BigDecimal("100.00"), JbhMoneyUtils.withJBHDecimals(new BigDecimal("100")));

    assertEquals(new BigDecimal("100.00"), JbhMoneyUtils.withJBHDecimals(new BigDecimal("100.00")));

    assertEquals(new BigDecimal("100.00"), JbhMoneyUtils.withJBHDecimals(new BigDecimal("100.0")));

    assertEquals(
        new BigDecimal("100.00"), JbhMoneyUtils.withJBHDecimals(new BigDecimal("100.000")));
    assertEquals(null, JbhMoneyUtils.withJBHDecimals((String) null));

    assertEquals(new BigDecimal("0.00"), JbhMoneyUtils.JBH_ZERO);
  }

  @Test
  public void shouldKnowIfIsZero() {
    assertTrue(JbhMoneyUtils.isZero(new BigDecimal("0.00")));

    assertTrue(JbhMoneyUtils.isZero(new BigDecimal("0")));

    assertTrue(JbhMoneyUtils.isZero(new BigDecimal("0.0")));

    assertTrue(JbhMoneyUtils.isZero(new BigDecimal("0.000")));

    assertTrue(JbhMoneyUtils.isZero(null));
  }

  @Test
  public void shouldKnowIfIsNotZero() {
    assertTrue(JbhMoneyUtils.isNotZero(new BigDecimal("0.01")));

    assertTrue(JbhMoneyUtils.isNotZero(new BigDecimal("1")));

    assertTrue(JbhMoneyUtils.isNotZero(new BigDecimal("1.0")));

    assertTrue(JbhMoneyUtils.isNotZero(new BigDecimal("1.000")));
    assertFalse(JbhMoneyUtils.isNotZero(null));
  }
}
