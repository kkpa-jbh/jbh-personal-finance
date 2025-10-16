package com.jbh.account.domain.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class MoneyUtilsTest {

  @Test
  public void shouldConvertToJBHDecimals() {
    assertEquals(new BigDecimal("100.00"), MoneyUtils.withJBHDecimals(new BigDecimal("100")));

    assertEquals(new BigDecimal("100.00"), MoneyUtils.withJBHDecimals(new BigDecimal("100.00")));

    assertEquals(new BigDecimal("100.00"), MoneyUtils.withJBHDecimals(new BigDecimal("100.0")));

    assertEquals(new BigDecimal("100.00"), MoneyUtils.withJBHDecimals(new BigDecimal("100.000")));
    assertEquals(null, MoneyUtils.withJBHDecimals((String) null));

    assertEquals(new BigDecimal("0.00"), MoneyUtils.JBH_ZERO);
  }

  @Test
  public void shouldKnowIfIsZero() {
    assertTrue(MoneyUtils.isZero(new BigDecimal("0.00")));

    assertTrue(MoneyUtils.isZero(new BigDecimal("0")));

    assertTrue(MoneyUtils.isZero(new BigDecimal("0.0")));

    assertTrue(MoneyUtils.isZero(new BigDecimal("0.000")));

    assertTrue(MoneyUtils.isZero(null));
  }

  @Test
  public void shouldKnowIfIsNotZero() {
    assertTrue(MoneyUtils.isNotZero(new BigDecimal("0.01")));

    assertTrue(MoneyUtils.isNotZero(new BigDecimal("1")));

    assertTrue(MoneyUtils.isNotZero(new BigDecimal("1.0")));

    assertTrue(MoneyUtils.isNotZero(new BigDecimal("1.000")));
    assertFalse(MoneyUtils.isNotZero(null));
  }
}
