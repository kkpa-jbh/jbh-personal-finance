package com.jbh.products.application.feature.movement.commands;

import com.jbh.products.domain.movement.vo.MovementCategoryVO;
import com.jbh.products.domain.movement.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Value object for adding a movement with date and amount.
 *
 * <p>Use the builder pattern for creating instances:
 *
 * <pre>{@code
 * // Simple movement with auto-derived type
 * AddMovementCommand.withCategory(date, amount, category)
 *     .build();
 *
 * // With description
 * AddMovementCommand.withCategory(date, amount, category)
 *     .description("Optional description")
 *     .build();
 *
 * // With balance snapshot
 * AddMovementCommand.withBalanceSnapshot(date, snapshot, category)
 *     .totalAmount(amount)
 *     .build();
 *
 * // Full control
 * AddMovementCommand.builder()
 *     .entryDate(date)
 *     .totalAmount(amount)
 *     .balanceSnapshot(snapshot)
 *     .movementType(MovementType.DEPOSIT)
 *     .categoryDTO(category)
 *     .description("Full control")
 *     .build();
 * }</pre>
 *
 * @param entryDate Date of the movement
 * @param totalAmount Positive always. Based on the movement type it will be interpreted as a
 *     deposit or a withdrawal (Optional when balanceSnapshot is not null)
 * @param balanceSnapshot Current net flow after the movement (optional)
 * @param movementType The type of movement
 * @param categoryDTO The category of the movement
 * @param description Optional description for the movement
 */
@SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName", "PMD.AvoidDuplicateLiterals"})
public record AddMovementCommand(
    LocalDate entryDate,
    BigDecimal totalAmount,
    BigDecimal balanceSnapshot,
    MovementType movementType,
    MovementCategoryVO categoryDTO,
    String description) {

  /**
   * Creates a new builder instance for fluent construction.
   *
   * @return a new Builder instance
   */
  public static MovementCommandBuilder builder() {
    return new MovementCommandBuilder();
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

  /** Manual builder implementation for AddMovementCommand. */
  @SuppressWarnings({"PMD.AvoidFieldNameMatchingMethodName", "PMD.AvoidDuplicateLiterals"})
  public static final class MovementCommandBuilder {
    private LocalDate entryDate;
    private BigDecimal totalAmount;
    private BigDecimal balanceSnapshot;
    private MovementType movementType;
    private MovementCategoryVO categoryDTO;
    private String description;

    private MovementCommandBuilder() {}

    public MovementCommandBuilder entryDate(final LocalDate entryDate) {
      this.entryDate = entryDate;
      return this;
    }

    public MovementCommandBuilder totalAmount(final BigDecimal totalAmount) {
      this.totalAmount = totalAmount;
      return this;
    }

    public MovementCommandBuilder balanceSnapshot(final BigDecimal balanceSnapshot) {
      this.balanceSnapshot = balanceSnapshot;
      return this;
    }

    public MovementCommandBuilder movementType(final MovementType movementType) {
      this.movementType = movementType;
      return this;
    }

    public MovementCommandBuilder categoryDTO(final MovementCategoryVO categoryDTO) {
      this.categoryDTO = categoryDTO;
      return this;
    }

    public MovementCommandBuilder description(final String description) {
      this.description = description;
      return this;
    }

    public AddMovementCommand build() {
      return new AddMovementCommand(
          entryDate, totalAmount, balanceSnapshot, movementType, categoryDTO, description);
    }
  }
}
