package com.jbh.products.application.core.vo.commands;

import static com.jbh.products.application.builders.commands.AddMovementCommandTestBuilder.withBalanceSnapshot;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.products.application.builders.commands.AddMovementCommandTestBuilder;
import com.jbh.products.application.feature.movement.commands.AddMovementCommand;
import com.jbh.products.domain.movement.vo.ExpenseCategory;
import com.jbh.products.domain.movement.vo.IncomeCategory;
import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.movement.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("AddMovementCommand Builder Tests")
class AddMovementCommandTest {

  private static final LocalDate TEST_DATE = LocalDate.of(2024, 1, 15);
  private static final BigDecimal TEST_AMOUNT = new BigDecimal("100.00");
  private static final BigDecimal TEST_SNAPSHOT = new BigDecimal("500.00");
  private static final MovementCategoryVO INCOME_CATEGORY =
      MovementCategoryVO.withType(IncomeCategory.SALARY);
  private static final MovementCategoryVO EXPENSE_CATEGORY =
      MovementCategoryVO.withType(ExpenseCategory.PERSONAL);

  @Nested
  @DisplayName("Builder Construction Tests")
  class BuilderConstructionTests {

    @Test
    @DisplayName("Should build command with all parameters using full builder")
    void shouldBuildWithAllParameters() {
      final AddMovementCommand command =
          AddMovementCommand.builder()
              .entryDate(TEST_DATE)
              .totalAmount(TEST_AMOUNT)
              .balanceSnapshot(TEST_SNAPSHOT)
              .movementType(MovementType.DEPOSIT)
              .categoryDTO(INCOME_CATEGORY)
              .description("Test description")
              .build();

      assertEquals(TEST_DATE, command.entryDate());
      assertEquals(TEST_AMOUNT, command.totalAmount());
      assertEquals(TEST_SNAPSHOT, command.balanceSnapshot());
      assertEquals(MovementType.DEPOSIT, command.movementType());
      assertEquals(INCOME_CATEGORY, command.categoryDTO());
      assertEquals("Test description", command.description());
    }

    @Test
    @DisplayName("Should build command with category auto-derivation")
    void shouldBuildWithCategoryAutoDerivedType() {
      final AddMovementCommand command =
          AddMovementCommandTestBuilder.withCategory(TEST_DATE, TEST_AMOUNT, INCOME_CATEGORY);

      assertEquals(TEST_DATE, command.entryDate());
      assertEquals(TEST_AMOUNT, command.totalAmount());
      assertNull(command.balanceSnapshot());
      assertEquals(MovementType.DEPOSIT, command.movementType());
      assertEquals(INCOME_CATEGORY, command.categoryDTO());
      assertNull(command.description());
    }

    @Test
    @DisplayName("Should build command with balance snapshot")
    void shouldBuildWithBalanceSnapshot() {
      final AddMovementCommand command =
          withBalanceSnapshot(TEST_DATE, TEST_SNAPSHOT, EXPENSE_CATEGORY, TEST_AMOUNT);
      assertEquals(TEST_DATE, command.entryDate());
      assertEquals(TEST_AMOUNT, command.totalAmount());
      assertEquals(TEST_SNAPSHOT, command.balanceSnapshot());
      assertEquals(MovementType.WITHDRAWAL, command.movementType());
      assertEquals(EXPENSE_CATEGORY, command.categoryDTO());
    }

    @Test
    @DisplayName("Should auto-derive WITHDRAWAL type from expense category")
    void shouldAutoDeriveWithdrawalType() {
      final AddMovementCommand command =
          AddMovementCommandTestBuilder.withCategory(TEST_DATE, TEST_AMOUNT, EXPENSE_CATEGORY);

      assertEquals(MovementType.WITHDRAWAL, command.movementType());
    }

    @Test
    @DisplayName("Should auto-derive DEPOSIT type from income category")
    void shouldAutoDeriveDepositType() {
      final AddMovementCommand command =
          AddMovementCommandTestBuilder.withCategory(TEST_DATE, TEST_AMOUNT, INCOME_CATEGORY);

      assertEquals(MovementType.DEPOSIT, command.movementType());
    }
  }

  @Nested
  @DisplayName("Validation Tests")
  class ValidationTests {

    @Test
    @DisplayName("Should fail validation when entry date is null")
    void shouldFailValidationWhenEntryDateIsNull() {
      final AddMovementCommand command =
          AddMovementCommand.builder()
              .totalAmount(TEST_AMOUNT)
              .movementType(MovementType.DEPOSIT)
              .categoryDTO(INCOME_CATEGORY)
              .build();

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, command::validate);
      assertEquals("Entry date cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation when movement type is null")
    void shouldFailValidationWhenMovementTypeIsNull() {
      final AddMovementCommand command =
          AddMovementCommand.builder()
              .entryDate(TEST_DATE)
              .totalAmount(TEST_AMOUNT)
              .categoryDTO(INCOME_CATEGORY)
              .build();

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, command::validate);
      assertEquals("Movement type cannot be null", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation when both amounts are null")
    void shouldFailValidationWhenBothAmountsAreNull() {
      final AddMovementCommand command =
          AddMovementCommand.builder()
              .entryDate(TEST_DATE)
              .movementType(MovementType.DEPOSIT)
              .categoryDTO(INCOME_CATEGORY)
              .build();

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, command::validate);
      assertEquals("There is not any amount to add", exception.getMessage());
    }

    @Test
    @DisplayName("Should fail validation when total amount is negative")
    void shouldFailValidationWhenTotalAmountIsNegative() {
      final AddMovementCommand command =
          AddMovementCommand.builder()
              .entryDate(TEST_DATE)
              .totalAmount(new BigDecimal("-100.00"))
              .movementType(MovementType.DEPOSIT)
              .categoryDTO(INCOME_CATEGORY)
              .build();

      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, command::validate);
      assertEquals("Total amount cannot be negative", exception.getMessage());
    }

    @Test
    @DisplayName("Should pass validation with valid total amount only")
    void shouldPassValidationWithTotalAmountOnly() {
      final AddMovementCommand command =
          AddMovementCommandTestBuilder.withCategory(TEST_DATE, TEST_AMOUNT, INCOME_CATEGORY);

      command.validate();

      assertEquals(TEST_AMOUNT, command.totalAmount());
      assertNull(command.balanceSnapshot());
    }

    @Test
    @DisplayName("Should pass validation with valid balance snapshot only")
    void shouldPassValidationWithBalanceSnapshotOnly() {
      final AddMovementCommand command =
          withBalanceSnapshot(TEST_DATE, TEST_SNAPSHOT, INCOME_CATEGORY, null);

      command.validate();

      assertEquals(TEST_SNAPSHOT, command.balanceSnapshot());
      assertNull(command.totalAmount());
    }

    @Test
    @DisplayName("Should pass validation with both amounts")
    void shouldPassValidationWithBothAmounts() {
      final AddMovementCommand command =
          withBalanceSnapshot(TEST_DATE, TEST_SNAPSHOT, INCOME_CATEGORY, TEST_AMOUNT);

      command.validate();

      assertEquals(TEST_AMOUNT, command.totalAmount());
      assertEquals(TEST_SNAPSHOT, command.balanceSnapshot());
    }

    @Test
    @DisplayName("Should pass validation with zero total amount")
    void shouldPassValidationWithZeroTotalAmount() {
      final AddMovementCommand command =
          AddMovementCommandTestBuilder.withCategory(TEST_DATE, BigDecimal.ZERO, INCOME_CATEGORY);

      command.validate();

      assertEquals(0, command.totalAmount().compareTo(BigDecimal.ZERO));
    }
  }

  @Nested
  @DisplayName("Builder Fluency Tests")
  class BuilderFluencyTests {

    @Test
    @DisplayName("Should support method chaining")
    void shouldSupportMethodChaining() {
      final AddMovementCommand command =
          AddMovementCommand.builder()
              .entryDate(TEST_DATE)
              .totalAmount(TEST_AMOUNT)
              .balanceSnapshot(TEST_SNAPSHOT)
              .movementType(MovementType.DEPOSIT)
              .categoryDTO(INCOME_CATEGORY)
              .description("Chained")
              .build();

      assertNotNull(command);
      assertEquals("Chained", command.description());
    }

    @Test
    @DisplayName("Should allow adding total amount to withBalanceSnapshot builder")
    void shouldAllowAddingTotalAmountToWithBalanceSnapshotBuilder() {
      final AddMovementCommand command =
          withBalanceSnapshot(TEST_DATE, TEST_SNAPSHOT, INCOME_CATEGORY, TEST_AMOUNT);
      assertEquals(TEST_AMOUNT, command.totalAmount());
      assertEquals(TEST_SNAPSHOT, command.balanceSnapshot());
    }
  }
}
