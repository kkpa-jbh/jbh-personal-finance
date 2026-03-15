package com.jbh.finance.test.testfixtures.builders.commands;

import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.INCOME_INITIAL_BALANCE;
import static com.jbh.finance.test.testfixtures.CategoryFixturesTestApp.PERSONAL;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.feature.movement.commands.AddMovementCommand;
import com.jbh.finance.domain.category.vo.CategoryTypeVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.test.testfixtures.CategoryFixturesTestApp;
import java.math.BigDecimal;
import java.time.LocalDate;

public class AddMovementCommandFixtureBuilder {

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
      final CategoryDTO categoryDTO) {
    return new AddMovementCommand(
        entryDate, totalAmount, balanceSnapshot, movementType, categoryDTO, null);
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
      final CategoryDTO categoryDTO) {
    return AddMovementCommand.builder()
        .entryDate(entryDate)
        .totalAmount(totalAmount)
        .movementType(movementType)
        .categoryDTO(categoryDTO)
        .build();
  }

  /**
   * Creates a builder for a movement with balance snapshot. Movement type is automatically derived
   * from the category. Use this when you have a balance snapshot instead of or in addition to a
   * total amount.
   *
   * @param entryDate the date of the movement
   * @param balanceSnapshot the balance after the movement
   * @param categoryDTO the category of the movement
   * @return a Builder instance pre-configured with these parameters
   */
  public static AddMovementCommand withBalanceSnapshot(
      final LocalDate entryDate,
      final BigDecimal balanceSnapshot,
      final CategoryDTO categoryDTO,
      final BigDecimal totalAmount) {
    return AddMovementCommand.builder()
        .entryDate(entryDate)
        .balanceSnapshot(balanceSnapshot)
        .categoryDTO(categoryDTO)
        .totalAmount(totalAmount)
        .movementType(MovementType.findByCategory(categoryDTO.getCategoryType()))
        .build();
  }

  public static AddMovementCommand createDepositIncome(
      final LocalDate date, final BigDecimal amount) {
    return withCategory(date, amount, CategoryFixturesTestApp.INCOME_DEPOSIT);
  }

  /**
   * Creates a builder for a movement with category auto-derivation. Movement type is automatically
   * derived from the category. This is the most common construction pattern.
   *
   * @param entryDate the date of the movement
   * @param totalAmount the amount of the movement
   * @param categoryDTO the category of the movement
   * @return a Builder instance pre-configured with these parameters
   */
  public static AddMovementCommand withCategory(
      final LocalDate entryDate, final BigDecimal totalAmount, final CategoryDTO categoryDTO) {
    return AddMovementCommand.builder()
        .entryDate(entryDate)
        .totalAmount(totalAmount)
        .categoryDTO(categoryDTO)
        .movementType(MovementType.findByCategory(categoryDTO.getCategoryType()))
        .build();
  }

  public static AddMovementCommand createDepositIncomeWithSnapshot(
      final LocalDate entryDate, final BigDecimal totalAmount, final BigDecimal balanceSnapshot) {
    return AddMovementCommand.builder()
        .entryDate(entryDate)
        .totalAmount(totalAmount)
        .categoryDTO(CategoryFixturesTestApp.INCOME_DEPOSIT)
        .balanceSnapshot(balanceSnapshot)
        .movementType(
            MovementType.findByCategory(CategoryFixturesTestApp.INCOME_DEPOSIT.getCategoryType()))
        .build();
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
      final LocalDate date, final BigDecimal amount, final CategoryTypeVO expenseCategory) {
    return AddMovementCommandFixtureBuilder.withCategory(
        date, amount, CategoryDTO.withInternalPurpose(expenseCategory, null));
  }

  public static AddMovementCommand createPersonalExpense(
      final LocalDate date, final BigDecimal amount) {
    return AddMovementCommandFixtureBuilder.withCategory(date, amount, PERSONAL);
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
    return AddMovementCommandFixtureBuilder.withCategory(date, amount, INCOME_INITIAL_BALANCE);
  }
}
