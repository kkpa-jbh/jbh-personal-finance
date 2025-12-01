package com.jbh.account.application.builders;

import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.CreateProductCommand;
import com.jbh.account.application.core.vo.commands.ExternalAccountInfoVO;
import com.jbh.account.application.core.vo.commands.LiquidateAccountCommand;
import com.jbh.account.domain.vo.AccountPK;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductMetadata;
import com.jbh.account.domain.vo.ProductMetadataKey;
import com.jbh.account.domain.vo.ProductType;
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
    metadata.getCreditCard().putCreditLimit(creditLimit);
    metadata.getCreditCard().putPaymentDueDay(paymentDueDay);
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
    metadata.getLoan().putPrincipalAmount(new BigDecimal("10000"));
    metadata.getLoan().putTotalAmountPaid(new BigDecimal("2000"));
    metadata.getLoan().putPayoffAmountToday(new BigDecimal("8500"));
    return new CreateProductCommand(userId, name, ProductType.LOAN, metadata);
  }

  public static CreateProductCommand createMockRealStateCommand(final UUID userId) {
    final ProductMetadata metadata = ProductMetadata.empty();
    metadata.getRealEstate().putPurchaseDate(LocalDate.now());
    metadata.getRealEstate().putPurchasePrice(new BigDecimal("100000"));
    metadata.getRealEstate().putPropertySize(new BigDecimal("100"));
    metadata.getRealEstate().putFinancedAmount(new BigDecimal("80000"));
    metadata.getRealEstate().putDownPaymentAmount(new BigDecimal("20000"));
    metadata.getRealEstate().putDownPaymentPercentage(new BigDecimal("20"));
    metadata.getRealEstate().putDownPaymentPaidToDate(new BigDecimal("0"));
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
    metadata.getInvestment().putBrokerName(brokerName);
    metadata.getInvestment().putCommissionRate(new BigDecimal("1.2"));
    return new CreateProductCommand(userId, name, ProductType.INVESTMENT, metadata);
  }

  /**
   * Creates an AddMovementCommand with all parameters (full constructor).
   *
   * @param entryDate the date of the movement
   * @param totalAmount the amount of the movement
   * @param balanceSnapshot the balance after the movement
   * @param movementType the type of movement
   * @param categoryDTO the category of the movement
   * @return an AddMovementCommand
   */
  public static AddMovementCommand createMovement(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryDTO categoryDTO) {
    return new AddMovementCommand(
        entryDate, totalAmount, balanceSnapshot, movementType, categoryDTO);
  }

  // ==================== AddMovementCommand Factory Methods ====================

  /**
   * Creates an AddMovementCommand without balance snapshot (3-param + category).
   *
   * @param entryDate the date of the movement
   * @param totalAmount the amount of the movement
   * @param categoryDTO the category of the movement
   * @return an AddMovementCommand
   */
  public static AddMovementCommand createMovement(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementCategoryDTO categoryDTO) {
    return new AddMovementCommand(entryDate, totalAmount, categoryDTO);
  }

  /**
   * Creates an AddMovementCommand with balance snapshot (infers movement type from category).
   *
   * @param entryDate the date of the movement
   * @param totalAmount the amount of the movement
   * @param balanceSnapshot the balance after the movement
   * @param categoryDTO the category of the movement
   * @return an AddMovementCommand
   */
  public static AddMovementCommand createMovementWithSnapshot(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementCategoryDTO categoryDTO) {
    return new AddMovementCommand(entryDate, totalAmount, balanceSnapshot, categoryDTO);
  }

  /**
   * Creates an AddMovementCommand with explicit movement type (no balance snapshot).
   *
   * @param entryDate the date of the movement
   * @param totalAmount the amount of the movement
   * @param movementType the type of movement
   * @param categoryDTO the category of the movement
   * @return an AddMovementCommand
   */
  public static AddMovementCommand createMovementWithType(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementType movementType,
      final MovementCategoryDTO categoryDTO) {
    return new AddMovementCommand(entryDate, totalAmount, movementType, categoryDTO);
  }

  /**
   * Creates an income movement (deposit).
   *
   * @param date the date of the movement
   * @param amount the amount
   * @param incomeCategory the income category
   * @return an AddMovementCommand for income
   */
  public static AddMovementCommand createIncome(
      final LocalDate date, final BigDecimal amount, final IncomeCategory incomeCategory) {
    return new AddMovementCommand(date, amount, MovementCategoryDTO.withType(incomeCategory));
  }

  // ==================== Convenience Methods for Common Scenarios ====================

  public static AddMovementCommand createDepositIncome(
      final LocalDate date, final BigDecimal amount) {
    return new AddMovementCommand(
        date, amount, MovementCategoryDTO.withType(IncomeCategory.DEPOSIT));
  }

  /**
   * Creates an expense movement (withdrawal).
   *
   * @param date the date of the movement
   * @param amount the amount
   * @param expenseCategory the expense category
   * @return an AddMovementCommand for expense
   */
  public static AddMovementCommand createExpense(
      final LocalDate date, final BigDecimal amount, final ExpenseCategory expenseCategory) {
    return new AddMovementCommand(date, amount, MovementCategoryDTO.withType(expenseCategory));
  }

  public static AddMovementCommand createPersonalExpense(
      final LocalDate date, final BigDecimal amount) {
    return new AddMovementCommand(
        date, amount, MovementCategoryDTO.withType(ExpenseCategory.PERSONAL));
  }

  /**
   * Creates an initial balance movement.
   *
   * @param date the date of the movement
   * @param amount the initial balance amount
   * @return an AddMovementCommand for initial balance
   */
  public static AddMovementCommand createInitialBalance(
      final LocalDate date, final BigDecimal amount) {
    return new AddMovementCommand(
        date, amount, MovementCategoryDTO.withType(IncomeCategory.INITIAL_BALANCE));
  }

  /**
   * Creates a dividend movement.
   *
   * @param date the date of the movement
   * @param amount the dividend amount
   * @return an AddMovementCommand for dividends
   */
  public static AddMovementCommand createDividend(final LocalDate date, final BigDecimal amount) {
    return new AddMovementCommand(
        date, amount, MovementCategoryDTO.withType(IncomeCategory.DIVIDENDS));
  }

  /**
   * Creates a transfer-out expense movement.
   *
   * @param date the date of the transfer
   * @param amount the transfer amount
   * @return an AddMovementCommand for transfer out
   */
  public static AddMovementCommand createTransferOut(
      final LocalDate date, final BigDecimal amount) {
    return new AddMovementCommand(
        date, amount, MovementCategoryDTO.withType(ExpenseCategory.TRANSFER));
  }

  /**
   * Creates a transfer-in income movement.
   *
   * @param date the date of the transfer
   * @param amount the transfer amount
   * @return an AddMovementCommand for transfer in
   */
  public static AddMovementCommand createTransferIn(final LocalDate date, final BigDecimal amount) {
    return new AddMovementCommand(
        date, amount, MovementCategoryDTO.withType(IncomeCategory.TRANSFER));
  }

  public static LiquidateAccountCommand createLiquidateCommandToInternal(
      final AccountPK accountPK, final BigDecimal amount, final LocalDate date) {
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
