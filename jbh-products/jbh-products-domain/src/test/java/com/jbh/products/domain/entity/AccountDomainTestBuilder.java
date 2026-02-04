package com.jbh.products.domain.entity;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.commons.exception.BusinessException;
import com.jbh.products.domain.vo.ProductId;
import com.jbh.products.domain.vo.ProductMetadata;
import com.jbh.products.domain.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Test builder for creating AccountDomain instances with basic movement data for existing products
 * This builder is intended for test purposes only and should not be used in production code.
 */
public class AccountDomainTestBuilder {

  private ProductId accountId;
  private UUID userId;
  private BigDecimal movementBalance;
  private BigDecimal currentBalance;
  private ProductType productType;
  private String name;
  private ProductMetadata productMetadata = ProductMetadata.empty();

  private AccountDomainTestBuilder() {
    this.productType = ProductType.SAVINGS; // Default to SAVINGS
  }

  /**
   * Convenience method to maintain backward compatibility with the old static factory method.
   * Creates an AccountDomain with basic movement data for an existing account.
   *
   * @param accountId Account identifier
   * @param userId User identifier
   * @param movementBalance Movement balance
   * @param currentBalance Current balance
   * @return AccountDomain instance
   * @deprecated Use {@link #createSavingProductWithBalance(ProductId, UUID, BigDecimal,
   *     BigDecimal)} instead
   */
  @Deprecated
  public static ProductDomain withBasicMovementForExisting(
      final ProductId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance) {
    return createSavingProductWithBalance(accountId, userId, movementBalance, currentBalance);
  }

  /**
   * Creates a SAVINGS ProductDomain with basic movement data for an existing account.
   *
   * @param accountId Account identifier
   * @param userId User identifier
   * @param movementBalance Movement balance
   * @param currentBalance Current balance
   * @return ProductDomain instance of type SAVINGS
   */
  public static ProductDomain createSavingProductWithBalance(
      final ProductId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance) {
    return builder()
        .withAccountId(accountId)
        .withUserId(userId)
        .withMovementBalance(movementBalance)
        .withCurrentBalance(currentBalance)
        .withProductType(ProductType.SAVINGS)
        .build();
  }

  /**
   * Builds an AccountDomain instance with basic movement data for an existing account. This method
   * uses reflection to set private fields since it's for testing purposes.
   *
   * @return AccountDomain instance configured with the builder's data
   */
  public ProductDomain build() {

    return createProduct(
        "DEFAULT_ACCOUNT_NAME",
        productType,
        userId,
        movementBalance,
        currentBalance,
        productMetadata);
  }

  public AccountDomainTestBuilder withProductType(final ProductType productType) {
    this.productType = productType;
    return this;
  }

  public AccountDomainTestBuilder withCurrentBalance(final BigDecimal currentBalance) {
    this.currentBalance = currentBalance;
    return this;
  }

  public AccountDomainTestBuilder withMovementBalance(final BigDecimal movementBalance) {
    this.movementBalance = movementBalance;
    return this;
  }

  public AccountDomainTestBuilder withUserId(final UUID userId) {
    this.userId = userId;
    return this;
  }

  public AccountDomainTestBuilder withAccountId(final ProductId accountId) {
    this.accountId = accountId;
    return this;
  }

  public static AccountDomainTestBuilder builder() {
    return new AccountDomainTestBuilder();
  }

  public static ProductDomain createProduct(
      final String name,
      final ProductType productType,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance,
      final ProductMetadata metadata) {
    return new ProductDomain(
        ProductId.generate(),
        name,
        productType,
        userId,
        movementBalance,
        currentBalance,
        JBH_ZERO,
        true,
        LocalDateTime.now(),
        LocalDateTime.now(),
        JBH_ZERO,
        metadata);
  }

  public static ProductDomain createLoanProduct(
      final UUID userId, final String name, final ProductMetadata metadata) {
    return createProduct("Loan Account", ProductType.LOAN, userId, JBH_ZERO, JBH_ZERO, metadata);
  }

  public static ProductDomain createRealEstateProduct(
      final UUID userId, final ProductMetadata metadata) throws BusinessException {
    return ProductDomain.withMinimumDataForCreation(
        "RE Account", ProductType.REAL_ESTATE_INVESTMENT, userId, metadata);
  }

  /**
   * Creates a CREDIT_CARD ProductDomain with basic movement data for an existing account.
   *
   * @param accountId Account identifier
   * @param userId User identifier
   * @param movementBalance Movement balance
   * @param currentBalance Current balance
   * @return ProductDomain instance of type CREDIT_CARD
   */
  public static ProductDomain createCreditCardProductWithBalance(
      final ProductId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance) {
    return builder()
        .withAccountId(accountId)
        .withUserId(userId)
        .withMovementBalance(movementBalance)
        .withCurrentBalance(currentBalance)
        .withProductType(ProductType.CREDIT_CARD)
        .build();
  }

  /**
   * Creates an INVESTMENT ProductDomain with basic movement data for an existing account.
   *
   * @param accountId Account identifier
   * @param userId User identifier
   * @param movementBalance Movement balance
   * @param currentBalance Current balance
   * @return ProductDomain instance of type INVESTMENT
   */
  public static ProductDomain createInvestmentProductWithBalance(
      final ProductId accountId,
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance,
      final ProductMetadata metadata) {
    return builder()
        .withAccountId(accountId)
        .withUserId(userId)
        .withMovementBalance(movementBalance)
        .withCurrentBalance(currentBalance)
        .withProductType(ProductType.INVESTMENT)
        .withProductMetadata(metadata)
        .build();
  }

  public AccountDomainTestBuilder withProductMetadata(final ProductMetadata productMetadata) {
    this.productMetadata = productMetadata;
    return this;
  }

  /**
   * Creates a CDT ProductDomain with basic movement data for an existing account.
   *
   * @param userId User identifier
   * @param movementBalance Movement balance
   * @param currentBalance Current balance
   * @return ProductDomain instance of type CDT
   */
  public static ProductDomain createCdtProductWithBalance(
      final UUID userId,
      final BigDecimal movementBalance,
      final BigDecimal currentBalance,
      final ProductMetadata productMetadata) {
    return builder()
        .withAccountId(ProductId.generate())
        .withUserId(userId)
        .withMovementBalance(movementBalance)
        .withCurrentBalance(currentBalance)
        .withProductType(ProductType.CDT)
        .withProductMetadata(productMetadata)
        .build();
  }

  public AccountDomainTestBuilder withName(final String name) {
    this.name = name;
    return this;
  }
}
