package com.jbh.finance.application.feature.movement.commands;

import com.jbh.finance.application.feature.category.dto.CategoryDTO;
import com.jbh.finance.application.shared.validation.CommandValidator;
import com.jbh.finance.domain.movement.vo.MovementReviewVO;
import com.jbh.finance.domain.movement.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * @param entryDate
 * @param movementType
 * @param totalAmount Always positive value. Based on the movement type it will be interpreted as a
 *     deposit or a withdrawal
 * @param balanceSnapshot
 * @param category the category, or {@code null} to use Other income / Unknown expense
 * @param description the movement text, or {@code null}
 * @param statementImportId the statement import that created the movement, or {@code null}
 * @param review confidence and needs-review of a statement row, or {@code null}
 */
public record AddMovementUploadedFileCommand(
    LocalDate entryDate,
    MovementType movementType,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    CategoryDTO category,
    String description,
    UUID statementImportId,
    MovementReviewVO review)
    implements CommandValidator {

  public AddMovementUploadedFileCommand(
      final LocalDate entryDate,
      final MovementType movementType,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot) {
    this(entryDate, movementType, totalAmount, balanceSnapshot, null, null, null, null);
  }

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
