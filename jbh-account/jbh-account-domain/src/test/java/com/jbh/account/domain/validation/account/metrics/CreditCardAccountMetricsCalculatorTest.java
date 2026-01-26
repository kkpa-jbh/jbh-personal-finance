package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.domain.entity.AccountDomainTestBuilder;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.commons.exception.BusinessException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CreditCardAccountMetricsCalculatorTest {

  private CreditCardAccountMetricsCalculator calculator;
  private UUID userId;

  @BeforeEach
  void setUp() {
    calculator = new CreditCardAccountMetricsCalculator();
    userId = UUID.randomUUID();
  }

  @Test
  void shouldCalculateProfitBalanceWhenCurrentBalanceIsGreaterThanMovementBalance() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("1000.00");
    final BigDecimal currentBalance = new BigDecimal("1500.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, movementBalance, currentBalance);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("500.00"), result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenCurrentBalanceIsLessThanMovementBalance() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("2000.00");
    final BigDecimal currentBalance = new BigDecimal("1500.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, movementBalance, currentBalance);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("-500.00"), result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenBothBalancesAreZero() {
    // Given
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, JBH_ZERO);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenCurrentBalanceIsZeroAndMovementBalanceIsPositive() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("1000.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, movementBalance, JBH_ZERO);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("-1000.00"), result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithPositiveGrowth() throws BusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("1000.00");
    final BigDecimal currentBalance = new BigDecimal("1500.00");
    final BigDecimal movementAmount = new BigDecimal("200.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithNegativeGrowth() throws BusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("2000.00");
    final BigDecimal currentBalance = new BigDecimal("1500.00");
    final BigDecimal movementAmount = new BigDecimal("100.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenOpeningBalanceIsZero() throws BusinessException {
    // Given
    final BigDecimal openingBalance = JBH_ZERO;
    final BigDecimal currentBalance = new BigDecimal("500.00");
    final BigDecimal movementAmount = new BigDecimal("500.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithZeroMovementAmount() throws BusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("1000.00");
    final BigDecimal currentBalance = new BigDecimal("1200.00");
    final BigDecimal movementAmount = JBH_ZERO;
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithNegativeCurrentBalance() throws BusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("1000.00");
    final BigDecimal currentBalance = new BigDecimal("-500.00");
    final BigDecimal movementAmount = new BigDecimal("200.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCreditCardProductWithBalance(
            ProductId.generate(), userId, JBH_ZERO, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }
}
