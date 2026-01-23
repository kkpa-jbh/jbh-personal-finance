package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.domain.entity.AccountDomainTestBuilder;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductMetadata;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CdtAccountMetricsCalculatorTest {

  private CdtAccountMetricsCalculator calculator;
  private UUID userId;

  @BeforeEach
  void setUp() {
    calculator = new CdtAccountMetricsCalculator();
    userId = UUID.randomUUID();
  }

  @Test
  void shouldCalculateProfitBalanceWhenFullyWithdrawn() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("-5000.00");

    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getCommon().putIsFullyWithdrawn(true);

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementBalance, JBH_ZERO, metadata);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("5000.00"), result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenNotFullyWithdrawn() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = new BigDecimal("5500.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementBalance, currentBalance, ProductMetadata.empty());

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenFullyWithdrawnWithPositiveMovementBalance() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = JBH_ZERO;

    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getCommon().putIsFullyWithdrawn(true);

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementBalance, currentBalance, metadata);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("5000.00"), result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenClosingBalanceIsNegative() throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal currentBalance = new BigDecimal("-100.00");
    final BigDecimal movementAmount = new BigDecimal("-10100.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementAmount, currentBalance, ProductMetadata.empty());

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenClosingBalanceIsZero() throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = JBH_ZERO;
    final BigDecimal movementAmount = new BigDecimal("-5000.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementAmount, currentBalance, ProductMetadata.empty());

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenClosingBalanceIsPositive() throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = new BigDecimal("5500.00");
    final BigDecimal movementAmount = new BigDecimal("300.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementAmount, currentBalance, ProductMetadata.empty());

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithNegativeMovementAmount() throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal currentBalance = new BigDecimal("-500.00");
    final BigDecimal movementAmount = new BigDecimal("-10500.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementAmount, currentBalance, ProductMetadata.empty());

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithZeroOpeningBalance() throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = JBH_ZERO;
    final BigDecimal currentBalance = JBH_ZERO;
    final BigDecimal movementAmount = JBH_ZERO;

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, movementAmount, currentBalance, ProductMetadata.empty());

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldReturnZeroProfitBalanceWhenNotFullyWithdrawnWithZeroBalances() {
    // Given
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createCdtProductWithBalance(
            userId, JBH_ZERO, JBH_ZERO, ProductMetadata.empty());

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }
}
