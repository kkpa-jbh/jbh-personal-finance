package com.jbh.products.application.core.vo.commands;

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
   * @deprecated Use {@link #builder()} or convenience builders like {@link #withCategory(LocalDate,
   *     BigDecimal, MovementCategoryVO)} instead
   */
  @Deprecated(since = "2026-02-05", forRemoval = true)
  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementType movementType,
      final MovementCategoryVO categoryDTO) {

    this(entryDate, totalAmount, null, movementType, categoryDTO, null);
  }

  /**
   * @deprecated Use {@link #withBalanceSnapshot(LocalDate, BigDecimal, MovementCategoryVO)} instead
   */
  @Deprecated(since = "2026-02-05", forRemoval = true)
  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementCategoryVO movementCategoryDTO) {
    this(
        entryDate,
        totalAmount,
        balanceSnapshot,
        MovementType.findByCategory(movementCategoryDTO),
        movementCategoryDTO,
        null);
  }

  /**
   * @deprecated Use {@link #withCategory(LocalDate, BigDecimal, MovementCategoryVO)} instead
   */
  @Deprecated(since = "2026-02-05", forRemoval = true)
  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementCategoryVO movementCategoryDTO) {
    this(
        entryDate,
        totalAmount,
        null,
        MovementType.findByCategory(movementCategoryDTO),
        movementCategoryDTO,
        null);
  }

  /**
   * @deprecated Use {@link #withFullControl(LocalDate, BigDecimal, BigDecimal, MovementType,
   *     MovementCategoryVO)} instead
   */
  @Deprecated(since = "2026-02-05", forRemoval = true)
  public AddMovementCommand(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryVO category) {
    this(entryDate, totalAmount, balanceSnapshot, movementType, category, null);
  }

  /**
   * Creates a new builder instance for fluent construction.
   *
   * @return a new Builder instance
   */
  public static MovementCommandBuilder builder() {
    return new MovementCommandBuilder();
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
  public static MovementCommandBuilder withCategory(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final MovementCategoryVO categoryDTO) {
    return new MovementCommandBuilder()
        .entryDate(entryDate)
        .totalAmount(totalAmount)
        .categoryDTO(categoryDTO)
        .movementType(MovementType.findByCategory(categoryDTO));
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
  public static MovementCommandBuilder withBalanceSnapshot(
      final LocalDate entryDate,
      final BigDecimal balanceSnapshot,
      final MovementCategoryVO categoryDTO) {
    return new MovementCommandBuilder()
        .entryDate(entryDate)
        .balanceSnapshot(balanceSnapshot)
        .categoryDTO(categoryDTO)
        .movementType(MovementType.findByCategory(categoryDTO));
  }

  /**
   * Creates a builder with full control over all parameters. Use this when you need to explicitly
   * specify the movement type or when the automatic derivation is not suitable.
   *
   * @param entryDate the date of the movement
   * @param totalAmount the amount of the movement
   * @param balanceSnapshot the balance after the movement
   * @param movementType the type of movement
   * @param categoryDTO the category of the movement
   * @return a Builder instance pre-configured with these parameters
   */
  public static MovementCommandBuilder withFullControl(
      final LocalDate entryDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryVO categoryDTO) {
    return new MovementCommandBuilder()
        .entryDate(entryDate)
        .totalAmount(totalAmount)
        .balanceSnapshot(balanceSnapshot)
        .movementType(movementType)
        .categoryDTO(categoryDTO);
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
