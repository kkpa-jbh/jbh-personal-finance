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
import java.util.UUID;
import org.junit.jupiter.api.Test;

public class ProductDomainTest {

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
    final ProductMetadata metadata1 = ProductMetadata.empty();
    metadata1.putMaturityDate(null);
    // Act
    assertThrows(
        AccountBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                accountName, ProductType.CDT, testUserId, metadata1));

    final ProductMetadata metadata2 = ProductMetadata.empty();
    metadata2.putMaturityDate(null); // This will be replaced with string in validation
    assertThrows(
        AccountBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                accountName, ProductType.CDT, testUserId, metadata2));

    final ProductMetadata metadata3 = ProductMetadata.empty();
    metadata3.putMaturityDate(LocalDate.of(2025, 1, 30));
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CDT, testUserId, metadata3);
    assertNotNull(account);
  }

  // ========== CREDIT CARD ACCOUNT VALIDATION TESTS ==========

  @Test
  public void shouldFailWhenCreditCardAccountMissingCreditLimit() {
    // Arrange
    final String accountName = "Credit Card Without Limit";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putPaymentDueDay(15);
    // Missing CREDIT_LIMIT

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasInvalidCreditLimitType() {
    // Arrange
    // Note: With PUT methods, this test is no longer relevant because putCreditLimit
    // only accepts BigDecimal. The type safety is enforced at compile time.
    // We can remove this test or adapt it to test a different validation scenario.
    final String accountName = "Credit Card Invalid Type";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    // Type safety is now enforced by the PUT method signature
    // metadata.putCreditLimit("1000"); // This would not compile
    metadata.putPaymentDueDay(15);

    // Act & Assert - Testing with missing credit limit instead
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasZeroCreditLimit() {
    // Arrange
    final String accountName = "Credit Card Zero Limit";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(BigDecimal.ZERO);
    metadata.putPaymentDueDay(15);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasNegativeCreditLimit() {
    // Arrange
    final String accountName = "Credit Card Negative Limit";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(new BigDecimal("-1000"));
    metadata.putPaymentDueDay(15);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountMissingPaymentDueDay() {
    // Arrange
    final String accountName = "Credit Card Without Due Day";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(new BigDecimal("10000"));
    // Missing PAYMENT_DUE_DAY

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasPaymentDueDayLessThanOne() {
    // Arrange
    final String accountName = "Credit Card Invalid Day Low";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(new BigDecimal("10000"));
    metadata.putPaymentDueDay(0);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasPaymentDueDayGreaterThan31() {
    // Arrange
    final String accountName = "Credit Card Invalid Day High";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(new BigDecimal("10000"));
    metadata.putPaymentDueDay(32);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenCreditCardAccountHasEmptyMetadata() {
    // Arrange
    final String accountName = "Credit Card Empty Metadata";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  // ========== CREDIT CARD ACCOUNT SUCCESS TESTS ==========

  @Test
  public void shouldCreateCreditCardAccountWithValidMetadata() throws AccountBusinessException {
    // Arrange
    final String accountName = "LULO Credit Card";
    final UUID testUserId = UUID.randomUUID();
    final BigDecimal creditLimit = new BigDecimal("1000000");
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(creditLimit);
    metadata.putPaymentDueDay(15);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, metadata);

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
    assertEquals(creditLimit, account.getMetadata().getCreditLimit());
    assertEquals(15, account.getMetadata().getPaymentDueDay());
  }

  @Test
  public void shouldCreateCreditCardAccountWithPaymentDueDayBoundaryValue1()
      throws AccountBusinessException {
    // Arrange - Test lower boundary (day 1)
    final String accountName = "Credit Card Day 1";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(new BigDecimal("50000"));
    metadata.putPaymentDueDay(1);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, metadata);

    // Assert
    assertNotNull(account);
    assertEquals(1, account.getMetadata().getPaymentDueDay());
  }

  @Test
  public void shouldCreateCreditCardAccountWithPaymentDueDayBoundaryValue31()
      throws AccountBusinessException {
    // Arrange - Test upper boundary (day 31)
    final String accountName = "Credit Card Day 31";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putCreditLimit(new BigDecimal("50000"));
    metadata.putPaymentDueDay(31);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, metadata);

    // Assert
    assertNotNull(account);
    assertEquals(31, account.getMetadata().getPaymentDueDay());
  }

  // ========== INVESTMENT ACCOUNT VALIDATION TESTS ==========

  @Test
  public void shouldFailWhenInvestmentAccountMissingBrokerName() {
    // Arrange
    final String accountName = "Investment Without Broker";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    // Missing BROKER_NAME

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenInvestmentAccountHasNullBrokerName() {
    // Arrange
    final String accountName = "Investment Null Broker";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putBrokerName(null);

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenInvestmentAccountHasBlankBrokerName() {
    // Arrange
    final String accountName = "Investment Blank Broker";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putBrokerName("   ");

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  @Test
  public void shouldFailWhenInvestmentAccountHasEmptyBrokerName() {
    // Arrange
    final String accountName = "Investment Empty Broker";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putBrokerName("");

    // Act & Assert
    final AccountBusinessException exception =
        assertThrows(
            AccountBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  // ========== INVESTMENT ACCOUNT SUCCESS TESTS ==========

  @Test
  public void shouldCreateInvestmentAccountWithValidBrokerName() throws AccountBusinessException {
    // Arrange
    final String accountName = "Fidelity Portfolio";
    final UUID testUserId = UUID.randomUUID();
    final String brokerName = "Fidelity";
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putBrokerName(brokerName);
    metadata.putCommissionRate(JBH_ZERO);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.INVESTMENT, testUserId, metadata);

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
    assertEquals(brokerName, account.getMetadata().getBrokerName());
  }

  @Test
  public void shouldCreateInvestmentAccountWithDifferentBrokerNames()
      throws AccountBusinessException {
    // Arrange
    final UUID testUserId = UUID.randomUUID();
    final String[] brokerNames = {"Charles Schwab", "Vanguard", "Interactive Brokers"};

    for (final String brokerName : brokerNames) {
      final ProductMetadata metadata = ProductMetadata.empty();
      metadata.putCommissionRate(new BigDecimal("1.30"));
      metadata.putBrokerName(brokerName);

      // Act
      final ProductDomain account =
          ProductDomain.withMinimumDataForCreation(
              brokerName + " Account", ProductType.INVESTMENT, testUserId, metadata);

      // Assert
      assertNotNull(account);
      assertEquals(brokerName, account.getMetadata().getBrokerName());
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
        AccountDomainTestBuilder.createProduct(
            "name", ProductType.SAVINGS, userId, JBH_ZERO, JBH_ZERO, ProductMetadata.empty());

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
  public void shouldCreateLoanProduct() throws AccountBusinessException {
    // Arrange
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.putLoanPrincipalAmount(new BigDecimal("5000"));
    metadata.putLoanInterestRate(new BigDecimal("5.5"));
    metadata.putLoanTotalAmountPaid(withJBHDecimals("4000"));
    metadata.putLoanPayoffAmountToday(withJBHDecimals("12000"));

    final ProductDomain productCreated =
        ProductDomain.withMinimumDataForCreation(
            "Personal Loan", ProductType.LOAN, UUID.randomUUID(), metadata);

    assertNotNull(productCreated.getId());
  }
}
