package com.jbh.finance.application.builders;

import com.jbh.finance.application.feature.movement.commands.ExternalAccountInfoVO;
import com.jbh.finance.application.feature.movement.commands.LiquidateAccountCommand;
import com.jbh.finance.application.feature.product.commands.CreateProductCommand;
import com.jbh.finance.domain.product.vo.ProductMetadata;
import com.jbh.finance.domain.product.vo.ProductMetadataKey;
import com.jbh.finance.domain.product.vo.ProductPK;
import com.jbh.finance.domain.product.vo.ProductType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Test Data Factory for creating command objects with sensible defaults. Centralizes command
 * creation to make tests more maintainable when command structure changes.
 */
public class CommandTestBuilder {

  private static final String DEFAULT_ACCOUNT_NAME = "Test Account";

  /**
   * Creates a CreateBasicAccountCommand with default values and empty metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param type the account type
   * @return a CreateBasicAccountCommand
   */
  public static CreateProductCommand createBasicAccountCommand(
      final UUID userId, final String name, final ProductType type) {
    return new CreateProductCommand(userId, name, type, ProductMetadata.empty());
  }

  /**
   * Creates a CreateBasicAccountCommand with metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param type the account type
   * @param metadata the account metadata
   * @return a CreateBasicAccountCommand
   */
  public static CreateProductCommand createBasicAccountCommand(
      final UUID userId,
      final String name,
      final ProductType type,
      final Map<ProductMetadataKey, Object> metadata) {
    return new CreateProductCommand(userId, name, type, ProductMetadata.fromMap(metadata));
  }

  public static CreateProductCommand createSavingAccountCommand(final UUID userId) {
    return createBasicAccountCommand(userId, ProductType.SAVINGS);
  }

  /**
   * Creates a CreateBasicAccountCommand with default name and empty metadata.
   *
   * @param userId the user ID
   * @param type the account type
   * @return a CreateBasicAccountCommand
   */
  public static CreateProductCommand createBasicAccountCommand(
      final UUID userId, final ProductType type) {
    return new CreateProductCommand(userId, DEFAULT_ACCOUNT_NAME, type, ProductMetadata.empty());
  }

  /**
   * Creates a CreateBasicAccountCommand for a credit card account with typical metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param creditLimit the credit limit
   * @param paymentDueDay the payment due day
   * @return a CreateBasicAccountCommand configured for credit card
   */
  public static CreateProductCommand createCreditCardCommand(
      final UUID userId,
      final String name,
      final BigDecimal creditLimit,
      final Integer paymentDueDay) {
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.findCreditCardMetadata().putCreditLimit(creditLimit);
    metadata.findCreditCardMetadata().putPaymentDueDay(paymentDueDay);
    return new CreateProductCommand(userId, name, ProductType.CREDIT_CARD, metadata);
  }

  public static CreateProductCommand createCreditCardCommand(
      final UUID userId, final String name, final ProductMetadata metadata) {
    return new CreateProductCommand(userId, name, ProductType.CREDIT_CARD, metadata);
  }

  public static CreateProductCommand createCDTCommand(
      final UUID userId, final String name, final ProductMetadata metadata) {
    return new CreateProductCommand(userId, name, ProductType.CDT, metadata);
  }

  public static CreateProductCommand createLoanCommand(
      final UUID userId, final String name, final ProductMetadata productMetadata) {
    return new CreateProductCommand(userId, name, ProductType.LOAN, productMetadata);
  }

  public static CreateProductCommand createMockLoanCommand(final UUID userId, final String name) {
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.findLoanMetadata().putPrincipalAmount(new BigDecimal("10000"));
    metadata.findLoanMetadata().putTotalAmountPaid(new BigDecimal("2000"));
    metadata.findLoanMetadata().putPayoffAmountToday(new BigDecimal("8500"));
    return new CreateProductCommand(userId, name, ProductType.LOAN, metadata);
  }

  public static CreateProductCommand createMockRealStateCommand(final UUID userId) {
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.findRealEstateMetadata().putPurchaseDate(LocalDate.now());
    metadata.findRealEstateMetadata().putPurchasePrice(new BigDecimal("100000"));
    metadata.findRealEstateMetadata().putPropertySize(new BigDecimal("100"));
    metadata.findRealEstateMetadata().putFinancedAmount(new BigDecimal("80000"));
    metadata.findRealEstateMetadata().putDownPaymentAmount(new BigDecimal("20000"));
    metadata.findRealEstateMetadata().putDownPaymentPercentage(new BigDecimal("20"));
    metadata.findRealEstateMetadata().putDownPaymentPaidToDate(new BigDecimal("0"));
    return new CreateProductCommand(
        userId, "Real State Account", ProductType.REAL_ESTATE_INVESTMENT, metadata);
  }

  /**
   * Creates a CreateBasicAccountCommand for an investment account with typical metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param brokerName the broker name
   * @return a CreateBasicAccountCommand configured for investment
   */
  public static CreateProductCommand createInvestmentCommand(
      final UUID userId, final String name, final String brokerName) {
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.findInvestmentMetadata().putBrokerName(brokerName);
    metadata.findInvestmentMetadata().putCommissionRate(new BigDecimal("1.2"));
    return new CreateProductCommand(userId, name, ProductType.INVESTMENT, metadata);
  }

  // ==================== Convenience Methods for Common Scenarios ====================

  public static LiquidateAccountCommand createLiquidateCommandToInternal(
      final ProductPK accountPK, final BigDecimal amount, final LocalDate date) {
    return new LiquidateAccountCommand(Optional.of(accountPK), Optional.empty(), amount, date);
  }

  public static LiquidateAccountCommand createLiquidateCommandToExternal(
      final ExternalAccountInfoVO externalAccountInfoVO,
      final BigDecimal amount,
      final LocalDate date) {
    return new LiquidateAccountCommand(
        Optional.empty(), Optional.of(externalAccountInfoVO), amount, date);
  }

  public static CreateProductCommand createRealEstateCommand(
      final UUID userId, final ProductMetadata metadata) {
    return new CreateProductCommand(
        userId, "MonteAzul", ProductType.REAL_ESTATE_INVESTMENT, metadata);
  }
}
