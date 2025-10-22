package com.jbh.account.application.builders;

import com.jbh.account.application.core.vo.commands.AddMovementCommand;
import com.jbh.account.application.core.vo.commands.CreateAccountCommand;
import com.jbh.account.domain.vo.AccountMetadataKey;
import com.jbh.account.domain.vo.AccountType;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementCategoryDTO;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
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
  public static CreateAccountCommand createBasicAccountCommand(
      final UUID userId, final String name, final AccountType type) {
    return new CreateAccountCommand(userId, name, type, Map.of());
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
  public static CreateAccountCommand createBasicAccountCommand(
      final UUID userId,
      final String name,
      final AccountType type,
      final Map<AccountMetadataKey, Object> metadata) {
    return new CreateAccountCommand(userId, name, type, metadata);
  }

  /**
   * Creates a CreateBasicAccountCommand with default name and empty metadata.
   *
   * @param userId the user ID
   * @param type the account type
   * @return a CreateBasicAccountCommand
   */
  public static CreateAccountCommand createBasicAccountCommand(
      final UUID userId, final AccountType type) {
    return new CreateAccountCommand(userId, DEFAULT_ACCOUNT_NAME, type, Map.of());
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
  public static CreateAccountCommand createCreditCardCommand(
      final UUID userId,
      final String name,
      final BigDecimal creditLimit,
      final Integer paymentDueDay) {
    final Map<AccountMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(AccountMetadataKey.CREDIT_LIMIT, creditLimit);
    metadata.put(AccountMetadataKey.PAYMENT_DUE_DAY, paymentDueDay);
    return new CreateAccountCommand(userId, name, AccountType.CREDIT_CARD, metadata);
  }

  /**
   * Creates a CreateBasicAccountCommand for an investment account with typical metadata.
   *
   * @param userId the user ID
   * @param name the account name
   * @param brokerName the broker name
   * @return a CreateBasicAccountCommand configured for investment
   */
  public static CreateAccountCommand createInvestmentCommand(
      final UUID userId, final String name, final String brokerName) {
    final Map<AccountMetadataKey, Object> metadata = new HashMap<>();
    metadata.put(AccountMetadataKey.BROKER_NAME, brokerName);
    return new CreateAccountCommand(userId, name, AccountType.INVESTMENT, metadata);
  }

  // ==================== AddMovementCommand Factory Methods ====================

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

  // ==================== Convenience Methods for Common Scenarios ====================

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
}
