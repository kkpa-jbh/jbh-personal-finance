package com.jbh.finance.test.application.feature.movement.commands;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.finance.application.feature.movement.commands.AddMovementUploadedFileCommand;
import com.jbh.finance.application.feature.movement.commands.UploadMovementsBatchCommand;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.shared.vo.EntityOperationVO;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class UploadMovementsBatchCommandTest {

  private final List<AddMovementUploadedFileCommand> movements =
      List.of(
          new AddMovementUploadedFileCommand(
              LocalDate.of(2026, 9, 1), MovementType.DEPOSIT, new BigDecimal("10.00"), null));

  @Test
  void shouldKeepFundsCheckWhenBatchIsForFile() {
    // Act
    final UploadMovementsBatchCommand batch = UploadMovementsBatchCommand.forFile(movements);

    // Assert
    assertSame(movements, batch.movements());
    assertFalse(batch.options().skipFundsCheck());
    assertFalse(batch.options().isMonthOfficiallyReported());
    assertEquals(EntityOperationVO.ADD, batch.options().operation());
  }

  @Test
  void shouldSkipFundsCheckWhenBatchIsForStatement() {
    // Act
    final UploadMovementsBatchCommand batch = UploadMovementsBatchCommand.forStatement(movements);

    // Assert
    assertSame(movements, batch.movements());
    assertTrue(batch.options().skipFundsCheck());
    assertFalse(batch.options().isMonthOfficiallyReported());
    assertEquals(EntityOperationVO.ADD, batch.options().operation());
  }

  @Test
  void shouldAcceptBatchWhenListHasMovements() {
    // Act & Assert
    UploadMovementsBatchCommand.forFile(movements).validate();
  }

  @Test
  void shouldRejectBatchWhenListIsNull() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class, () -> UploadMovementsBatchCommand.forFile(null).validate());
  }

  @Test
  void shouldRejectBatchWhenListIsEmpty() {
    // Act & Assert
    assertThrows(
        IllegalArgumentException.class,
        () -> UploadMovementsBatchCommand.forStatement(List.of()).validate());
  }
}
