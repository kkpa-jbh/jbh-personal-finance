package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.domain.entity.AccountDomainTestBuilder;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class InvestmentAccountMetricsCalculatorTest {

  private InvestmentAccountMetricsCalculator calculator;
  private UUID userId;

  @BeforeEach
  void setUp() {
    calculator = new InvestmentAccountMetricsCalculator();
    userId = UUID.randomUUID();
  }

  @Test
  void shouldCalculateProfitBalanceWhenCurrentBalanceIsGreaterThanMovementBalance() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = new BigDecimal("6500.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createInvestmentProductWithBalance(
            AccountId.generate(), userId, movementBalance, currentBalance);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("1500.00"), result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenCurrentBalanceIsLessThanMovementBalance() {
    // Given
    final BigDecimal movementBalance = new BigDecimal("10000.00");
    final BigDecimal currentBalance = new BigDecimal("8000.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createInvestmentProductWithBalance(
            AccountId.generate(), userId, movementBalance, currentBalance);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("-2000.00"), result);
  }

  @Test
  void shouldCalculateProfitBalanceWhenBothBalancesAreZero() {
    // Given
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createInvestmentProductWithBalance(
            AccountId.generate(), userId, JBH_ZERO, JBH_ZERO);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(accountDomain);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenNotFullyWithdrawn() throws AccountBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = new BigDecimal("6000.00");
    final BigDecimal movementAmount = new BigDecimal("500.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createInvestmentProductWithBalance(
            AccountId.generate(), userId, JBH_ZERO, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenFullyWithdrawnWithZeroBalance()
      throws AccountBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal currentBalance = JBH_ZERO;
    final BigDecimal movementAmount = new BigDecimal("-10000.00");

    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.IS_FULLY_WITHDRAWN, true);

    final ProductDomain accountDomain =
        new ProductDomain(
            AccountId.generate(),
            "Investment Account",
            ProductType.INVESTMENT,
            userId,
            movementAmount,
            currentBalance,
            JBH_ZERO,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            JBH_ZERO,
            ProductMetadata.of(metadata));

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenFullyWithdrawnWithNegativeBalance()
      throws AccountBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal currentBalance = new BigDecimal("-100.00");
    final BigDecimal movementAmount = new BigDecimal("-10100.00");

    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.IS_FULLY_WITHDRAWN, true);

    final ProductDomain accountDomain =
        new ProductDomain(
            AccountId.generate(),
            "Investment Account",
            ProductType.INVESTMENT,
            userId,
            movementAmount,
            currentBalance,
            JBH_ZERO,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            JBH_ZERO,
            ProductMetadata.of(metadata));

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenNotFullyWithdrawnWithZeroOpeningBalance()
      throws AccountBusinessException {
    // Given
    final BigDecimal openingBalance = JBH_ZERO;
    final BigDecimal currentBalance = new BigDecimal("1000.00");
    final BigDecimal movementAmount = new BigDecimal("1000.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createInvestmentProductWithBalance(
            AccountId.generate(), userId, movementAmount, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWhenFullyWithdrawnButCurrentBalanceIsPositive()
      throws AccountBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("5000.00");
    final BigDecimal currentBalance = new BigDecimal("100.00");
    final BigDecimal movementAmount = new BigDecimal("-4900.00");

    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.IS_FULLY_WITHDRAWN, true);

    final ProductDomain accountDomain =
        new ProductDomain(
            AccountId.generate(),
            "Investment Account",
            ProductType.INVESTMENT,
            userId,
            movementAmount,
            currentBalance,
            JBH_ZERO,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            JBH_ZERO,
            ProductMetadata.of(metadata));

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }

  @Test
  void shouldCalculateNetGrowthRateWithNegativeMovementAmount() throws AccountBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal currentBalance = new BigDecimal("8000.00");
    final BigDecimal movementAmount = new BigDecimal("-2000.00");

    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createInvestmentProductWithBalance(
            AccountId.generate(), userId, movementAmount, currentBalance);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, accountDomain, movementAmount);

    // Then
    assertNotNull(result);
  }
}
