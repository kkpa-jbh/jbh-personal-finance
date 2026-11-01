package com.jbh.finance.test.application.feature.movement.commands;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.domain.movement.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class AddMovementUploadedFileCommandTest {

  private static final LocalDate DATE = LocalDate.of(2026, 9, 1);
  private static final BigDecimal AMOUNT = new BigDecimal("10.00");

  @Test
  void shouldLeaveStatementFieldsNullWhenUsingThreeArgumentConstructor() {
    // Act
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(DATE, AMOUNT, null);

    // Assert
    assertEquals(MovementType.DEPOSIT, command.movementType());
    assertNull(command.category());
    assertNull(command.description());
    assertNull(command.statementImportId());
    assertNull(command.review());
  }

  @Test
  void shouldLeaveStatementFieldsNullWhenUsingFourArgumentConstructor() {
    // Act
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(DATE, MovementType.WITHDRAWAL, AMOUNT.negate(), null);

    // Assert
    assertEquals(MovementType.WITHDRAWAL, command.movementType());
    assertEquals(DATE, command.entryDate());
    assertNull(command.category());
    assertNull(command.description());
    assertNull(command.statementImportId());
    assertNull(command.review());
  }

  @Test
  void shouldPassValidationWhenDateAndAmountAreGiven() {
    // Arrange
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(DATE, MovementType.DEPOSIT, AMOUNT, null);

    // Act & Assert
    assertDoesNotThrow(command::validate);
  }

  @Test
  void shouldFailValidationWhenEntryDateIsNull() {
    // Arrange
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(null, MovementType.DEPOSIT, AMOUNT, null);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, command::validate);
  }

  @Test
  void shouldFailValidationWhenAmountAndSnapshotAreNull() {
    // Arrange
    final AddMovementUploadedFileCommand command =
        new AddMovementUploadedFileCommand(DATE, MovementType.DEPOSIT, null, null);

    // Act & Assert
    assertThrows(IllegalArgumentException.class, command::validate);
  }
}
