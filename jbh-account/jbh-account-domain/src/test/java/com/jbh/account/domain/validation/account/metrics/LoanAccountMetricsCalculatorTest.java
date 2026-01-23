package com.jbh.account.domain.validation.account.metrics;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jbh.account.domain.entity.AccountDomainTestBuilder;
import com.jbh.account.domain.entity.AccountMovementDomain;
import com.jbh.account.domain.entity.MovementCategoryDomain;
import com.jbh.account.domain.entity.ProductDomain;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.ProductId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductMetadata;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LoanAccountMetricsCalculatorTest {

  private LoanAccountMetricsCalculator calculator;
  private UUID userId;

  @BeforeEach
  void setUp() {
    calculator = new LoanAccountMetricsCalculator();
    userId = UUID.randomUUID();
  }

  @Test
  void shouldCalculateProfitBalanceAsZeroWithPositiveBalance() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(loanProduct);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateProfitBalanceAsZeroWithNegativeBalance() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(loanProduct);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateProfitBalanceAsZeroWithZeroBalance() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result = calculator.calculateProfitBalance(loanProduct);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateAsZeroWithPositiveValues() throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal movementAmount = new BigDecimal("500.00");
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, loanProduct, movementAmount);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateAsZeroWithNegativeMovementAmount()
      throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("10000.00");
    final BigDecimal movementAmount = new BigDecimal("-500.00");
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, loanProduct, movementAmount);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateAsZeroWithZeroOpeningBalance()
      throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = JBH_ZERO;
    final BigDecimal movementAmount = new BigDecimal("1000.00");
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, loanProduct, movementAmount);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldCalculateNetGrowthRateAsZeroWithZeroMovementAmount()
      throws ProductBusinessException {
    // Given
    final BigDecimal openingBalance = new BigDecimal("5000.00");
    final BigDecimal movementAmount = JBH_ZERO;
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    // When
    final BigDecimal result =
        calculator.calculateNetGrowthReate(openingBalance, loanProduct, movementAmount);

    // Then
    assertNotNull(result);
    assertEquals(JBH_ZERO, result);
  }

  @Test
  void shouldUpdateMetadataWhenLoanTotalAmountPaidExists() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getLoan().putTotalAmountPaid(new BigDecimal("1000.00"));

    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    final AccountMovementDomain movement =
        new AccountMovementDomain(
            ProductId.generate(),
            MovementType.WITHDRAWAL,
            LocalDate.now(),
            new BigDecimal("-500.00"),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withCategoryType(ExpenseCategory.PERSONAL));

    // When
    final ProductMetadata result = calculator.updateMetadata(loanProduct, movement);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("500.00"), result.getLoan().getTotalAmountPaid());
  }

  @Test
  void shouldUpdateMetadataWithPositiveMovementAmount() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getLoan().putTotalAmountPaid(new BigDecimal("2000.00"));

    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    final AccountMovementDomain movement =
        new AccountMovementDomain(
            ProductId.generate(),
            MovementType.DEPOSIT,
            LocalDate.now(),
            new BigDecimal("1000.00"),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withCategoryType(ExpenseCategory.PERSONAL));

    // When
    final ProductMetadata result = calculator.updateMetadata(loanProduct, movement);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("3000.00"), result.getLoan().getTotalAmountPaid());
  }

  @Test
  void shouldNotUpdateMetadataWhenLoanTotalAmountPaidDoesNotExist() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    final AccountMovementDomain movement =
        new AccountMovementDomain(
            ProductId.generate(),
            MovementType.WITHDRAWAL,
            LocalDate.now(),
            new BigDecimal("-500.00"),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withCategoryType(ExpenseCategory.PERSONAL));

    // When
    final ProductMetadata result = calculator.updateMetadata(loanProduct, movement);

    // Then
    assertNotNull(result);
    // Metadata should be returned but without LOAN_TOTAL_AMOUNT_PAID key
    assertEquals(metadata.asMap().size(), result.asMap().size());
  }

  @Test
  void shouldUpdateMetadataWithZeroInitialAmount() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getLoan().putTotalAmountPaid(JBH_ZERO);

    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    final AccountMovementDomain movement =
        new AccountMovementDomain(
            ProductId.generate(),
            MovementType.WITHDRAWAL,
            LocalDate.now(),
            new BigDecimal("-250.00"),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withCategoryType(ExpenseCategory.PERSONAL));

    // When
    final ProductMetadata result = calculator.updateMetadata(loanProduct, movement);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("-250.00"), result.getLoan().getTotalAmountPaid());
  }

  @Test
  void shouldUpdateMetadataWithLargeMovementAmount() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getLoan().putTotalAmountPaid(new BigDecimal("50000.00"));

    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    final AccountMovementDomain movement =
        new AccountMovementDomain(
            ProductId.generate(),
            MovementType.WITHDRAWAL,
            LocalDate.now(),
            new BigDecimal("-25000.00"),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withCategoryType(ExpenseCategory.PERSONAL));

    // When
    final ProductMetadata result = calculator.updateMetadata(loanProduct, movement);

    // Then
    assertNotNull(result);
    assertEquals(new BigDecimal("25000.00"), result.getLoan().getTotalAmountPaid());
  }

  @Test
  void shouldHandleDecimalPrecisionInMetadataUpdate() {
    // Given
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getLoan().putTotalAmountPaid(new BigDecimal("1000.123456"));

    final ProductDomain loanProduct =
        AccountDomainTestBuilder.createLoanProduct(userId, "Test Loan", metadata);

    final AccountMovementDomain movement =
        new AccountMovementDomain(
            ProductId.generate(),
            MovementType.WITHDRAWAL,
            LocalDate.now(),
            new BigDecimal("-500.654321"),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty(),
            MovementCategoryDomain.withCategoryType(ExpenseCategory.PERSONAL));

    // When
    final ProductMetadata result = calculator.updateMetadata(loanProduct, movement);

    // Then
    assertNotNull(result);
    // Result should be rounded to 2 decimal places by withJBHDecimals
    assertEquals(new BigDecimal("499.47"), result.getLoan().getTotalAmountPaid());
  }
}
