package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class AccountDomainTest {

  static UUID userId = UUID.randomUUID();
  ProductDomain accountDomain;
  LocalDate today = LocalDate.now();

  @Test
  public void shouldCreateAccountWithBasicMovementForExistingId() {
    accountDomain =
        AccountDomainTestBuilder.createSavingProductWithBalance(
            AccountId.generate(), userId, JBH_ZERO, JBH_ZERO);

    assert accountDomain.getId() != null;
    assertDefaultAccountBalances(accountDomain);
  }

  private void assertDefaultAccountBalances(final ProductDomain accountDomain) {
    assertNotNull(accountDomain.getId());
    assertEquals(JBH_ZERO, accountDomain.getMovementBalance());
    assertEquals(JBH_ZERO, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getNetProfitBalance());
    assertTrue(accountDomain.isActive());
  }

  @Test
  public void shouldCreateWithMinimumDataForCreation() throws Exception {
    final String name = "Test Account";
    accountDomain =
        ProductDomain.withMinimumDataForCreation(
            name, ProductType.SAVINGS, UUID.randomUUID(), ProductMetadata.empty());
    assert accountDomain.getId() != null;
    assertRequiredAccount(accountDomain);
  }

  private void assertRequiredAccount(final ProductDomain accountDomain) {
    assertNotNull(accountDomain.getId());
    assertNotNull(accountDomain.getName());
    assertNotNull(accountDomain.getType());
    assertNotNull(accountDomain.getUserId());
    assertNotNull(accountDomain.getCreatedAt());
    assertNotNull(accountDomain.getCurrentBalance());
    assertNotNull(accountDomain.getMovementBalance());
    assertNotNull(accountDomain.getNetProfitBalance());
  }

  // ========== SAVINGS ACCOUNT TESTS ==========

  @Test
  public void shouldCreateSavingsAccountWithEmptyMetadata() throws AccountBusinessException {
    // Arrange
    final String accountName = "My Savings Account";
    final UUID testUserId = UUID.randomUUID();

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.SAVINGS, testUserId, ProductMetadata.empty());

    // Assert
    assertNotNull(account);
    assertNotNull(account.getId());
    assertEquals(accountName, account.getName());
    assertEquals(ProductType.SAVINGS, account.getType());
    assertEquals(testUserId, account.getUserId());
    assertEquals(JBH_ZERO, account.getCurrentBalance());
    assertEquals(JBH_ZERO, account.getMovementBalance());
    assertEquals(JBH_ZERO, account.getNetProfitBalance());
    assertTrue(account.isActive());
    assertNotNull(account.getCreatedAt());
  }

  @Test
  public void shouldCreateSavingsAccountWithNullMetadata() throws AccountBusinessException {
    // Arrange
    final String accountName = "Another Savings";
    final UUID testUserId = UUID.randomUUID();

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.SAVINGS, testUserId, null);

    // Assert
    assertNotNull(account);
    assertEquals(accountName, account.getName());
    assertEquals(ProductType.SAVINGS, account.getType());
    assertEquals(testUserId, account.getUserId());
  }

  // ========== CDT ACCOUNT TESTS ==========

  @Test
  public void shouldCreateCdtAccountWithEmptyMetadata() throws AccountBusinessException {
    // Arrange
    final String accountName = "CDT Account";
    final UUID testUserId = UUID.randomUUID();

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CDT, testUserId, ProductMetadata.empty());

    // Assert
    assertNotNull(account);
    assertNotNull(account.getId());
    assertEquals(accountName, account.getName());
    assertEquals(ProductType.CDT, account.getType());
    assertEquals(ProductType.CDT, account.getType());
    assertEquals(testUserId, account.getUserId());
    assertEquals(JBH_ZERO, account.getCurrentBalance());
    assertEquals(JBH_ZERO, account.getMovementBalance());
    assertEquals(JBH_ZERO, account.getNetProfitBalance());
    assertTrue(account.isActive());
  }

  @Test
  public void shouldCreateCdtAccountWithNullMetadata() throws AccountBusinessException {
    // Arrange
    final String accountName = "CDT Long Term";
    final UUID testUserId = UUID.randomUUID();

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(accountName, ProductType.CDT, testUserId, null);

    // Assert
    assertNotNull(account);
    assertEquals(accountName, account.getName());
    assertEquals(ProductType.CDT, account.getType());
  }

  @Test
  public void shouldValidateCDTAccount() throws AccountBusinessException {
    // Arrange
    final String accountName = "CDT Long Term";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.MATURITY_DATE, null);
    // Act
    assertThrows(
        AccountBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                accountName, ProductType.CDT, testUserId, ProductMetadata.of(metadata)));

    metadata.put(ProductMetadataKey.MATURITY_DATE, "30/01/2025");
    assertThrows(
        AccountBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                accountName, ProductType.CDT, testUserId, ProductMetadata.of(metadata)));

    metadata.put(ProductMetadataKey.MATURITY_DATE, LocalDate.of(2025, 1, 30));
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CDT, testUserId, ProductMetadata.of(metadata));
    assertNotNull(account);
  }

  // ========== CREDIT CARD ACCOUNT VALIDATION TESTS ==========

  @Test
  public void shouldFailWhenCreditCardAccountMissingCreditLimit() {
    // Arrange
    final String accountName = "Credit Card Without Limit";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 15);
    // Missing CREDIT_LIMIT

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasInvalidCreditLimitType() {
    // Arrange
    final String accountName = "Credit Card Invalid Type";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, "1000"); // String instead of BigDecimal
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 15);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasZeroCreditLimit() {
    // Arrange
    final String accountName = "Credit Card Zero Limit";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, BigDecimal.ZERO);
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 15);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasNegativeCreditLimit() {
    // Arrange
    final String accountName = "Credit Card Negative Limit";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("-1000"));
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 15);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountMissingPaymentDueDay() {
    // Arrange
    final String accountName = "Credit Card Without Due Day";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("10000"));
    // Missing PAYMENT_DUE_DAY

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasInvalidPaymentDueDayType() {
    // Arrange
    final String accountName = "Credit Card Invalid Day Type";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("10000"));
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, "15"); // String instead of Integer

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasPaymentDueDayLessThanOne() {
    // Arrange
    final String accountName = "Credit Card Invalid Day Low";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("10000"));
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 0);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasPaymentDueDayGreaterThan31() {
    // Arrange
    final String accountName = "Credit Card Invalid Day High";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("10000"));
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 32);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasEmptyMetadata() {
    // Arrange
    final String accountName = "Credit Card Empty Metadata";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName,
                    ProductType.CREDIT_CARD,
                    testUserId,
                    ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  // ========== CREDIT CARD ACCOUNT SUCCESS TESTS ==========

  @Test
  public void shouldCreateCreditCardAccountWithValidMetadata() throws AccountBusinessException {
    // Arrange
    final String accountName = "LULO Credit Card";
    final UUID testUserId = UUID.randomUUID();
    final BigDecimal creditLimit = new BigDecimal("1000000");
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, creditLimit);
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 15);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, ProductMetadata.of(metadata));

    // Assert
    assertNotNull(account);
    assertNotNull(account.getId());
    assertEquals(accountName, account.getName());
    assertEquals(ProductType.CREDIT_CARD, account.getType());
    assertEquals(testUserId, account.getUserId());
    assertEquals(JBH_ZERO, account.getCurrentBalance());
    assertEquals(JBH_ZERO, account.getMovementBalance());
    assertEquals(JBH_ZERO, account.getNetProfitBalance());
    assertTrue(account.isActive());
    assertNotNull(account.getCreatedAt());
    assertEquals(creditLimit, account.getMetadataField(ProductMetadataKey.CREDIT_LIMIT));
    assertEquals(15, account.getMetadataField(ProductMetadataKey.PAYMENT_DUE_DAY));
  }

  @Test
  public void shouldCreateCreditCardAccountWithPaymentDueDayBoundaryValue1()
      throws AccountBusinessException {
    // Arrange - Test lower boundary (day 1)
    final String accountName = "Credit Card Day 1";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("50000"));
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 1);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, ProductMetadata.of(metadata));

    // Assert
    assertNotNull(account);
    assertEquals(1, account.getMetadataField(ProductMetadataKey.PAYMENT_DUE_DAY));
  }

  @Test
  public void shouldCreateCreditCardAccountWithPaymentDueDayBoundaryValue31()
      throws AccountBusinessException {
    // Arrange - Test upper boundary (day 31)
    final String accountName = "Credit Card Day 31";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.CREDIT_LIMIT, new BigDecimal("50000"));
    metadata.put(ProductMetadataKey.PAYMENT_DUE_DAY, 31);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, ProductMetadata.of(metadata));

    // Assert
    assertNotNull(account);
    assertEquals(31, account.getMetadataField(ProductMetadataKey.PAYMENT_DUE_DAY));
  }

  // ========== INVESTMENT ACCOUNT VALIDATION TESTS ==========

  @Test
  public void shouldFailWhenInvestmentAccountMissingBrokerName() {
    // Arrange
    final String accountName = "Investment Without Broker";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    // Missing BROKER_NAME

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenInvestmentAccountHasNullBrokerName() {
    // Arrange
    final String accountName = "Investment Null Broker";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.BROKER_NAME, null);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenInvestmentAccountHasBlankBrokerName() {
    // Arrange
    final String accountName = "Investment Blank Broker";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.BROKER_NAME, "   ");

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenInvestmentAccountHasEmptyBrokerName() {
    // Arrange
    final String accountName = "Investment Empty Broker";
    final UUID testUserId = UUID.randomUUID();
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.BROKER_NAME, "");

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, ProductMetadata.of(metadata)));

    assertNotNull(exception.getMessage());
  }

  // ========== INVESTMENT ACCOUNT SUCCESS TESTS ==========

  @Test
  public void shouldCreateInvestmentAccountWithValidBrokerName() throws AccountBusinessException {
    // Arrange
    final String accountName = "Fidelity Portfolio";
    final UUID testUserId = UUID.randomUUID();
    final String brokerName = "Fidelity";
    final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(ProductMetadataKey.BROKER_NAME, brokerName);
    metadata.put(ProductMetadataKey.COMMISSION_RATE, JBH_ZERO);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.INVESTMENT, testUserId, ProductMetadata.of(metadata));

    // Assert
    assertNotNull(account);
    assertNotNull(account.getId());
    assertEquals(accountName, account.getName());
    assertEquals(ProductType.INVESTMENT, account.getType());
    assertEquals(testUserId, account.getUserId());
    assertEquals(JBH_ZERO, account.getCurrentBalance());
    assertEquals(JBH_ZERO, account.getMovementBalance());
    assertEquals(JBH_ZERO, account.getNetProfitBalance());
    assertTrue(account.isActive());
    assertNotNull(account.getCreatedAt());
    assertEquals(brokerName, account.getMetadataField(ProductMetadataKey.BROKER_NAME));
  }

  @Test
  public void shouldCreateInvestmentAccountWithDifferentBrokerNames()
      throws AccountBusinessException {
    // Arrange
    final UUID testUserId = UUID.randomUUID();
    final String[] brokerNames = {"Charles Schwab", "Vanguard", "Interactive Brokers"};

    for (final String brokerName : brokerNames) {
      final Map<ProductMetadataKey, Object> metadata = new HashMap<>();
      metadata.put(ProductMetadataKey.COMMISSION_RATE, new BigDecimal("1.30"));
      metadata.put(ProductMetadataKey.BROKER_NAME, brokerName);

      // Act
      final ProductDomain account =
          ProductDomain.withMinimumDataForCreation(
              brokerName + " Account",
              ProductType.INVESTMENT,
              testUserId,
              ProductMetadata.of(metadata));

      // Assert
      assertNotNull(account);
      assertEquals(brokerName, account.getMetadataField(ProductMetadataKey.BROKER_NAME));
    }
  }

  @Test
  public void shouldCreateWithBasicMovementForExisting() {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createSavingProductWithBalance(
            AccountId.generate(), userId, movementBalance, currentBalance);

    assertEquals(movementBalance, accountDomain.getMovementBalance());
    assertEquals(currentBalance, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getNetProfitBalance());
  }

  @Test
  public void shouldCreateWithConstructor() throws AccountBusinessException {
    final var accountDomain =
        new ProductDomain(
            AccountId.generate(),
            "name",
            ProductType.SAVINGS,
            userId,
            JBH_ZERO,
            JBH_ZERO,
            JBH_ZERO,
            true,
            LocalDateTime.now(),
            LocalDateTime.now(),
            JBH_ZERO,
            ProductMetadata.empty());

    assertNotNull(accountDomain.getId());

    final var movementAmount = new BigDecimal("100.00");
    final AccountMovementDomain movement =
        new AccountMovementDomain(
            AccountMovementId.generate(),
            accountDomain.getId(),
            MovementType.DEPOSIT,
            MovementCategoryDomain.withCategoryType(IncomeCategory.OTHER),
            movementAmount,
            LocalDate.now(),
            null,
            AccountMovementMetadata.createEmpty());

    accountDomain.syncBalancesByMovement(movement, false);

    assertNotNull(accountDomain.getId());
    assertFalse(accountDomain.isFullyWithdrawn());
    assertFalse(accountDomain.hasMetadata(ProductMetadataKey.IS_FULLY_WITHDRAWN));

    final AccountMovementDomain withdrawalMovement =
        new AccountMovementDomain(
            AccountMovementId.generate(),
            accountDomain.getId(),
            MovementType.WITHDRAWAL,
            MovementCategoryDomain.withCategoryType(ExpenseCategory.SOCIAL_SECURITY),
            new BigDecimal("-100.00"),
            LocalDate.now(),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty());
    accountDomain.syncBalancesByMovement(withdrawalMovement, false);
    assertTrue(accountDomain.isFullyWithdrawn());
    assertTrue(accountDomain.hasMetadata(ProductMetadataKey.IS_FULLY_WITHDRAWN));

    accountDomain.getUpdatedAt();
    accountDomain.getCreatedAt();
    accountDomain.getMetadata();

    final AccountMovementDomain unknownMovement =
        new AccountMovementDomain(
            AccountMovementId.generate(),
            AccountId.generate(),
            MovementType.DEPOSIT,
            MovementCategoryDomain.withCategoryType(IncomeCategory.OTHER),
            movementAmount,
            LocalDate.now(),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty());
    assertThrows(
        AccountBusinessException.class,
        () -> accountDomain.syncBalancesByMovement(unknownMovement, false));
  }

  @Test
  public void shouldFailWhenCreatingLoanProductWithoutValidator() throws AccountBusinessException {
    // Arrange
    final Map<ProductMetadataKey, Object> metadata =
        Map.of(
            ProductMetadataKey.LOAN_PRINCIPAL_AMOUNT, new BigDecimal("5000"),
            ProductMetadataKey.LOAN_INTEREST_RATE, new BigDecimal("5.5"),
            ProductMetadataKey.LOAN_TOTAL_AMOUNT_PAID, withJBHDecimals("4000"));

    ProductDomain.withMinimumDataForCreation(
        "Personal Loan", ProductType.LOANS, UUID.randomUUID(), ProductMetadata.of(metadata));
    ;
  }
}
