package com.jbh.finance.domain.movement.vo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jbh.finance.domain.shared.vo.EntityOperationVO;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("unit")
class ProcessMovementOptionsVOTest {

  @Test
  void shouldSkipFundsCheckWhenOptionsAreForStatementImport() {
    // Act
    final ProcessMovementOptionsVO options = ProcessMovementOptionsVO.forStatementImport();

    // Assert
    assertTrue(options.skipFundsCheck());
    assertEquals(EntityOperationVO.ADD, options.operation());
    assertFalse(options.isMonthOfficiallyReported());
  }

  @Test
  void shouldNotSkipFundsCheckWhenOnlyOperationIsGiven() {
    // Act
    final ProcessMovementOptionsVO options = new ProcessMovementOptionsVO(EntityOperationVO.ADD);

    // Assert
    assertFalse(options.skipFundsCheck());
    assertEquals(EntityOperationVO.ADD, options.operation());
    assertFalse(options.isMonthOfficiallyReported());
  }

  @Test
  void shouldNotSkipFundsCheckWhenReportFlagAndOperationAreGiven() {
    // Act
    final ProcessMovementOptionsVO options =
        new ProcessMovementOptionsVO(false, EntityOperationVO.REMOVE);

    // Assert
    assertFalse(options.skipFundsCheck());
    assertEquals(EntityOperationVO.REMOVE, options.operation());
  }
}
