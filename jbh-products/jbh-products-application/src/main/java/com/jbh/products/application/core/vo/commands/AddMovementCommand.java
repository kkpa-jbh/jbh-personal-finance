package com.jbh.products.application.core.vo.commands;

import com.jbh.products.domain.vo.MovementCategoryDTO;
import com.jbh.products.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object for adding a movement with date and amount.
 *
 * @param entryDate Date of the movement
 * @param totalAmount Positive always. Based on the movement type it will be interpreted as a
 *     deposit or a withdrawal
 * @param balanceSnapshot Current net flow after the movement (optional)
 * @param movementType The type of movement
 * @param categoryDTO The category of the movement
 * @param description Optional description for the movement
 */
public record AddMovementCommand(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    MovementCategoryDTO categoryDTO,
    String description) {

  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementType movementType,
      final MovementCategoryDTO categoryDTO) {

    this(entryDate, totalAmount, null, movementType, categoryDTO, null);
  }

  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementCategoryDTO movementCategoryDTO) {
    this(
        entryDate,
        totalAmount,
        balanceSnapshot,
        MovementType.findByCategory(movementCategoryDTO),
        movementCategoryDTO,
        null);
  }

  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementCategoryDTO movementCategoryDTO) {
    this(
        entryDate,
        totalAmount,
        null,
        MovementType.findByCategory(movementCategoryDTO),
        movementCategoryDTO,
        null);
  }

  public AddMovementCommand(final LocalDate entryDate, final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot, final MovementType movementType, final MovementCategoryDTO category) {
  this (entryDate, totalAmount, balanceSnapshot, movementType, category, null);

  }

  public void validate() {
    validateEntryDate();
    validateMovementType();
    validateAmounts();
  }

  private void validateEntryDate() {
    if (entryDate == null) {
      throw new IllegalArgumentException("Entry date cannot be null");
    }
  }

  private void validateMovementType() {
    if (movementType == null) {
      throw new IllegalArgumentException("Movement type cannot be null");
    }
  }

  private void validateAmounts() {
    if (totalAmount == null && balanceSnapshot == null) {
      throw new IllegalArgumentException("There is not any amount to add");
    }
    if (totalAmount != null && totalAmount.signum() < 0) {
      throw new IllegalArgumentException("Total amount cannot be negative");
    }
  }

  @Override
  public String toString() {
    return String.format(
        " date:%s, category:%s, amount:%s, snapshot:%s",
        entryDate, categoryDTO, totalAmount, balanceSnapshot);
  }
}
