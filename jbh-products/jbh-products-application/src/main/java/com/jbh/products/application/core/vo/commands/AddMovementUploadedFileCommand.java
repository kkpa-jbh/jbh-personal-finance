package com.jbh.products.application.core.vo.commands;

import com.jbh.products.domain.movement.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * @param entryDate
 * @param movementType
 * @param totalAmount Always positive value. Based on the movement type it will be interpreted as a
 *     deposit or a withdrawal
 * @param balanceSnapshot
 */
public record AddMovementUploadedFileCommand(
    LocalDate entryDate,
    MovementType movementType,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot)
    implements CommandValidator {

  public AddMovementUploadedFileCommand(
      final LocalDate entryDate, final BigDecimal totalAmount, final BigDecimal balanceSnapshot) {
    this(
        entryDate,
        MovementType.findByTotalAmountAndBalanceSnapshot(totalAmount, balanceSnapshot),
        totalAmount,
        balanceSnapshot);
  }

  @Override
  public void validate() {
    if (entryDate == null) {
      throw new IllegalArgumentException("Entry date cannot be null");
    }
    if (totalAmount == null && balanceSnapshot == null) {
      throw new IllegalArgumentException("There is not any amount to add");
    }
  }
}
