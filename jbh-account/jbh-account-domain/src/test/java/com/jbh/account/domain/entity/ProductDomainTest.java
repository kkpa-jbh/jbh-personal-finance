package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductId;
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
            ProductId.generate(), userId, JBH_ZERO, JBH_ZERO);

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
  public void shouldCreateSavingsAccountWithEmptyMetadata() throws ProductBusinessException {
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
  public void shouldCreateSavingsAccountWithNullMetadata() throws ProductBusinessException {
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
  public void shouldCreateCdtAccountWithEmptyMetadata() throws ProductBusinessException {
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
  public void shouldCreateCdtAccountWithNullMetadata() throws ProductBusinessException {
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
  public void shouldValidateCDTAccount() throws ProductBusinessException {
    // Arrange
    final String accountName = "CDT Long Term";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata1 = ProductMetadata.empty();
    metadata1.getCdt().putMaturityDate(null);
    // Act
    assertThrows(
        ProductBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                accountName, ProductType.CDT, testUserId, metadata1));

    final ProductMetadata metadata2 = ProductMetadata.empty();
    metadata2.getCdt().putMaturityDate(null); // This will be replaced with string in validation
    assertThrows(
        ProductBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                accountName, ProductType.CDT, testUserId, metadata2));

    final ProductMetadata metadata3 = ProductMetadata.empty();
    metadata3.getCdt().putMaturityDate(LocalDate.of(2025, 1, 30));
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
    metadata.getCreditCard().putPaymentDueDay(15);
    // Missing CREDIT_LIMIT

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    // metadata.getCreditCard().putCreditLimit("1000"); // This would not compile
    metadata.getCreditCard().putPaymentDueDay(15);

    // Act & Assert - Testing with missing credit limit instead
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getCreditCard().putCreditLimit(BigDecimal.ZERO);
    metadata.getCreditCard().putPaymentDueDay(15);

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getCreditCard().putCreditLimit(new BigDecimal("-1000"));
    metadata.getCreditCard().putPaymentDueDay(15);

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getCreditCard().putCreditLimit(new BigDecimal("10000"));
    // Missing PAYMENT_DUE_DAY

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getCreditCard().putCreditLimit(new BigDecimal("10000"));
    metadata.getCreditCard().putPaymentDueDay(0);

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getCreditCard().putCreditLimit(new BigDecimal("10000"));
    metadata.getCreditCard().putPaymentDueDay(32);

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.CREDIT_CARD, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  // ========== CREDIT CARD ACCOUNT SUCCESS TESTS ==========

  @Test
  public void shouldCreateCreditCardAccountWithValidMetadata() throws ProductBusinessException {
    // Arrange
    final String accountName = "LULO Credit Card";
    final UUID testUserId = UUID.randomUUID();
    final BigDecimal creditLimit = new BigDecimal("1000000");
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getCreditCard().putCreditLimit(creditLimit);
    metadata.getCreditCard().putPaymentDueDay(15);

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
    assertEquals(0, creditLimit.compareTo(account.getMetadata().getCreditCard().getCreditLimit()));
    assertEquals(15, account.getMetadata().getCreditCard().getPaymentDueDay());
  }

  @Test
  public void shouldCreateCreditCardAccountWithPaymentDueDayBoundaryValue1()
      throws ProductBusinessException {
    // Arrange - Test lower boundary (day 1)
    final String accountName = "Credit Card Day 1";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getCreditCard().putCreditLimit(new BigDecimal("50000"));
    metadata.getCreditCard().putPaymentDueDay(1);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, metadata);

    // Assert
    assertNotNull(account);
    assertEquals(1, account.getMetadata().getCreditCard().getPaymentDueDay());
  }

  @Test
  public void shouldCreateCreditCardAccountWithPaymentDueDayBoundaryValue31()
      throws ProductBusinessException {
    // Arrange - Test upper boundary (day 31)
    final String accountName = "Credit Card Day 31";
    final UUID testUserId = UUID.randomUUID();
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getCreditCard().putCreditLimit(new BigDecimal("50000"));
    metadata.getCreditCard().putPaymentDueDay(31);

    // Act
    final ProductDomain account =
        ProductDomain.withMinimumDataForCreation(
            accountName, ProductType.CREDIT_CARD, testUserId, metadata);

    // Assert
    assertNotNull(account);
    assertEquals(31, account.getMetadata().getCreditCard().getPaymentDueDay());
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
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getInvestment().putBrokerName(null);

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getInvestment().putBrokerName("   ");

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
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
    metadata.getInvestment().putBrokerName("");

    // Act & Assert
    final ProductBusinessException exception =
        assertThrows(
            ProductBusinessException.class,
            () ->
                ProductDomain.withMinimumDataForCreation(
                    accountName, ProductType.INVESTMENT, testUserId, metadata));

    assertNotNull(exception.getMessage());
  }

  // ========== INVESTMENT ACCOUNT SUCCESS TESTS ==========

  @Test
  public void shouldCreateInvestmentAccountWithValidBrokerName() throws ProductBusinessException {
    // Arrange
    final String accountName = "Fidelity Portfolio";
    final UUID testUserId = UUID.randomUUID();
    final String brokerName = "Fidelity";
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getInvestment().putBrokerName(brokerName);
    metadata.getInvestment().putCommissionRate(JBH_ZERO);

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
    assertEquals(brokerName, account.getMetadata().getInvestment().getBrokerName());
  }

  @Test
  public void shouldCreateInvestmentAccountWithDifferentBrokerNames()
      throws ProductBusinessException {
    // Arrange
    final UUID testUserId = UUID.randomUUID();
    final String[] brokerNames = {"Charles Schwab", "Vanguard", "Interactive Brokers"};

    for (final String brokerName : brokerNames) {
      final ProductMetadata metadata = ProductMetadata.empty();
      metadata.getInvestment().putCommissionRate(new BigDecimal("1.30"));
      metadata.getInvestment().putBrokerName(brokerName);

      // Act
      final ProductDomain account =
          ProductDomain.withMinimumDataForCreation(
              brokerName + " Account", ProductType.INVESTMENT, testUserId, metadata);

      // Assert
      assertNotNull(account);
      assertEquals(brokerName, account.getMetadata().getInvestment().getBrokerName());
    }
  }

  @Test
  public void shouldCreateWithBasicMovementForExisting() {
    final var movementBalance = new BigDecimal("100.00");
    final var currentBalance = new BigDecimal("200.00");
    final ProductDomain accountDomain =
        AccountDomainTestBuilder.createSavingProductWithBalance(
            ProductId.generate(), userId, movementBalance, currentBalance);

    assertEquals(movementBalance, accountDomain.getMovementBalance());
    assertEquals(currentBalance, accountDomain.getCurrentBalance());
    assertEquals(JBH_ZERO, accountDomain.getNetProfitBalance());
  }

  @Test
  public void shouldCreateWithConstructor() throws ProductBusinessException {
    final var accountDomain =
        AccountDomainTestBuilder.createProduct(
            "name", ProductType.SAVINGS, userId, JBH_ZERO, JBH_ZERO, ProductMetadata.empty());

    assertNotNull(accountDomain.getId());

    final var movementAmount = new BigDecimal("100.00");
    final ProductMovementDomain movement =
        new ProductMovementDomain(
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
    assertFalse(accountDomain.hasMetadata(ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN));

    final ProductMovementDomain withdrawalMovement =
        new ProductMovementDomain(
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
    assertTrue(accountDomain.hasMetadata(ProductMetadataKey.COMMON_IS_FULLY_WITHDRAWN));

    accountDomain.getUpdatedAt();
    accountDomain.getCreatedAt();
    accountDomain.getMetadata();

    final ProductMovementDomain unknownMovement =
        new ProductMovementDomain(
            AccountMovementId.generate(),
            ProductId.generate(),
            MovementType.DEPOSIT,
            MovementCategoryDomain.withCategoryType(IncomeCategory.OTHER),
            movementAmount,
            LocalDate.now(),
            JBH_ZERO,
            AccountMovementMetadata.createEmpty());
    assertThrows(
        ProductBusinessException.class,
        () -> accountDomain.syncBalancesByMovement(unknownMovement, false));
  }

  @Test
  public void shouldCreateLoanProduct() throws ProductBusinessException {
    // Arrange
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getLoan().putPrincipalAmount(new BigDecimal("5000"));
    metadata.getLoan().putInterestRate(new BigDecimal("5.5"));
    metadata.getLoan().putTotalAmountPaid(withJBHDecimals("4000"));
    metadata.getLoan().putPayoffAmountToday(withJBHDecimals("12000"));

    final ProductDomain productCreated =
        ProductDomain.withMinimumDataForCreation(
            "LoanProduct", ProductType.LOAN, UUID.randomUUID(), metadata);

    assertNotNull(productCreated.getId());
  }

  @Test
  public void shouldCreateRealEstateProductWithoutRequiredMetadata()
      throws ProductBusinessException {
    // Arrange
    final ProductMetadata metadata = ProductMetadata.empty();

    assertThrows(
        ProductBusinessException.class,
        () ->
            ProductDomain.withMinimumDataForCreation(
                "RES ", ProductType.REAL_ESTATE_INVESTMENT, UUID.randomUUID(), metadata));
  }

  @Test
  public void shouldCreateRealEstateProduct() throws ProductBusinessException {
    // Arrange
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getRealEstate().putPurchaseDate(LocalDate.now());

    // Adding REAL_ESTATE_PURCHASE_PRICE
    metadata.getRealEstate().putPurchasePrice(new BigDecimal(1000));

    // Adding REAL_ESTATE_PROPERTY_SIZE
    metadata.getRealEstate().putPropertySize(new BigDecimal("100.00"));

    // Adding REAL_ESTATE_FINANCED_AMOUNT
    metadata.getRealEstate().putFinancedAmount(BigDecimal.ONE);

    // Adding REAL_ESTATE_DOWN_PAYMENT_PERCENTAGE
    metadata.getRealEstate().putDownPaymentPercentage(new BigDecimal("30.00"));

    // Adding REAL_ESTATE_DOWN_PAYMENT_PAID_TO_DATE
    metadata.getRealEstate().putDownPaymentPaidToDate(BigDecimal.ONE);
    final ProductDomain product =
        AccountDomainTestBuilder.createRealEstateProduct(UUID.randomUUID(), metadata);

    assertNotNull(product);

    product.validateInsufficientNetFlow(
        EntityBuilder.with(
            ProductId.generate(),
            LocalDate.now(),
            BigDecimal.ONE,
            BigDecimal.ZERO,
            MovementType.DEPOSIT,
            MovementCategoryDomain.withCategoryType(IncomeCategory.OTHER)));
  }
}
