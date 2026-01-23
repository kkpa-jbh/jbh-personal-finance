package com.jbh.account.domain.calculators;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.account.domain.exceptions.ProductBusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

public class MoneyGrowthCalculatorTest {

  MoneyGrowthCalculator calculator = new MoneyGrowthCalculator();

  @Test
  public void justForPrinting() {
    final BigDecimal opening = new BigDecimal("35276312");
    final BigDecimal closing = new BigDecimal("35693653");
    final BigDecimal movement = new BigDecimal("318629");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    System.out.println("Growth: " + growth);
  }

  private BigDecimal getCalculateGrowth(
      final BigDecimal opening, final BigDecimal closing, final BigDecimal movement) {
    try {
      return calculator.calculateGrowth(opening, closing, movement);
    } catch (final ProductBusinessException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  public void testCompletWithDrawal() {
    final BigDecimal opening = new BigDecimal("1981883");
    final BigDecimal closing = new BigDecimal("0");
    final BigDecimal movement = new BigDecimal("-3768488");

    assertThrows(
        ProductBusinessException.class,
        () -> calculator.calculateGrowth(opening, closing, movement));
  }

  @Test
  public void test1() {
    final BigDecimal opening = new BigDecimal("10000");
    final BigDecimal closing = new BigDecimal("10200");
    final BigDecimal movement = new BigDecimal("0");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("2.00"), growth);
  }

  @Test
  public void test2() {
    final BigDecimal opening = new BigDecimal("100");
    final BigDecimal closing = new BigDecimal("110");
    final BigDecimal movement = new BigDecimal("0");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("10.00"), growth);
  }

  @Test
  public void test3() {
    final BigDecimal opening = new BigDecimal("5000000");
    final BigDecimal closing = new BigDecimal("5022458.19");
    final BigDecimal movement = new BigDecimal("0");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("0.45"), growth);
  }

  @Test
  public void test4() {
    final BigDecimal opening = new BigDecimal("5000000");
    final BigDecimal closing = new BigDecimal("4978356.37");
    final BigDecimal movement = new BigDecimal("0");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("-0.43"), growth);
  }

  @Test
  public void test2_LargeDepositMidMonth() {
    final BigDecimal opening = new BigDecimal("10000");
    final BigDecimal closing = new BigDecimal("20100");
    final BigDecimal movement = new BigDecimal("10000");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("0.67"), growth);
  }

  @Test
  public void test3_WithdrawalDuringMonth() {
    final BigDecimal opening = new BigDecimal("10000");
    final BigDecimal closing = new BigDecimal("8150");
    final BigDecimal movement = new BigDecimal("-2000");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("1.67"), growth);
  }

  @Test
  public void test4_LossWithNoMovements() {
    final BigDecimal opening = new BigDecimal("10000");
    final BigDecimal closing = new BigDecimal("9500");
    final BigDecimal movement = new BigDecimal("0");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("-5.00"), growth);
  }

  @Test
  public void test5_MixedMovementsSmallGrowth() {
    final BigDecimal opening = new BigDecimal("5000");
    final BigDecimal closing = new BigDecimal("8060");
    final BigDecimal movement = new BigDecimal("3000");

    final BigDecimal growth = getCalculateGrowth(opening, closing, movement);
    assertEquals(new BigDecimal("0.92"), growth);
  }
}
