package com.jbh.finance.domain.movement.vo;

import com.jbh.finance.domain.shared.vo.EntityOperationVO;

/**
 * Options to process one movement.
 *
 * @param isMonthOfficiallyReported the month of the movement already has an official report
 * @param operation add or remove the movement
 * @param skipFundsCheck {@code true} to skip the insufficient funds check (statement imports)
 */
public record ProcessMovementOptionsVO(
    boolean isMonthOfficiallyReported, EntityOperationVO operation, boolean skipFundsCheck) {

  public ProcessMovementOptionsVO(
      final boolean isMonthOfficiallyReported, final EntityOperationVO operation) {
    this(isMonthOfficiallyReported, operation, false);
  }

  public ProcessMovementOptionsVO(final EntityOperationVO operation) {
    this(false, operation, false);
  }

  /**
   * Options for movements that come from a bank statement: add, month not officially reported and
   * no funds check, because the bank already accepted them.
   *
   * @return the options
   */
  public static ProcessMovementOptionsVO forStatementImport() {
    return new ProcessMovementOptionsVO(false, EntityOperationVO.ADD, true);
  }
}
