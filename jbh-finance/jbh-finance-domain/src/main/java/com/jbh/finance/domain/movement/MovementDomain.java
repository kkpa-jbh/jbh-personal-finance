package com.jbh.finance.domain.movement;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.finance.domain.category.CategoryDomain;
import com.jbh.finance.domain.movement.vo.MovementId;
import com.jbh.finance.domain.movement.vo.MovementMetadata;
import com.jbh.finance.domain.movement.vo.MovementMetadataKey;
import com.jbh.finance.domain.movement.vo.MovementType;
import com.jbh.finance.domain.product.vo.ProductId;
import com.jbh.finance.domain.shared.exceptions.BusinessDomainExceptionType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@SuppressWarnings({"PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal", "PMD.GodClass"})
public class MovementDomain {

  private final MovementId id;
  private final ProductId productId;
  private final MovementType movementType;
  private final CategoryDomain category;

  /**
   * For withdrawals, the amount is negative. For deposits, the amount is positive. For balance
   * snapshots, the amount indicates the type of movement (deposit, withdrawal.)
   */
  private final BigDecimal movementAmount;

  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;
  private final MovementMetadata metadata;
  private final String description;

  private final LocalDateTime createdAt;

  public MovementDomain(
      final ProductId productId,
      final MovementType movementType,
      final LocalDate movementDate,
      final BigDecimal movementAmount,
      final BigDecimal balanceSnapshot,
      final MovementMetadata metadata,
      final CategoryDomain category,
      final String description) {
    this(
        MovementId.generate(),
        productId,
        movementType,
        category,
        movementAmount,
        movementDate,
        balanceSnapshot,
        metadata,
        description);
  }

  public MovementDomain(
      final MovementId id,
      final ProductId productId,
      final MovementType movementType,
      final CategoryDomain category,
      final BigDecimal movementAmount,
      final LocalDate movementDate,
      final BigDecimal balanceSnapshot,
      final MovementMetadata metadata,
      final String description) {
    this.id = id;
    this.productId = productId;
    this.movementType = movementType;
    this.category = category;
    this.movementAmount = JbhMoneyUtils.withJBHDecimals(movementAmount);
    this.movementDate = movementDate;
    this.balanceSnapshot = JbhMoneyUtils.withJBHDecimals(balanceSnapshot);
    this.metadata = metadata;
    this.description = description;
    this.createdAt = LocalDateTime.now();
  }

  public void validate() throws BusinessException {
    validateAccountId();
    validateMovementType();
    validateMovementDate();
    validateCategory();
    validateMovementDateNotFuture();
    validateAmountOrSnapshot();
  }

  private void validateAccountId() {
    if (productId == null || productId.value() == null) {
      throw new GenericSpecificationException("Account ID cannot be null");
    }
  }

  private void validateMovementType() throws BusinessException {
    if (movementType == null) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENT_TYPE);
    }
    if (movementType.isBalanceSnapshot() && category != null) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_CATEGORY_BALANCE_SNAPSHOT);
    }
  }

  private void validateMovementDate() throws BusinessException {
    if (movementDate == null) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENT_TYPE);
    }
  }

  private void validateCategory() throws BusinessException {
    validateMovementType();
    validateCategoryRequirement();
    validateCategoryByMovementType();
  }

  private void validateMovementDateNotFuture() throws BusinessException {
    if (movementDate != null && movementDate.isAfter(LocalDate.now())) {
      throw new BusinessException(BusinessDomainExceptionType.FUTURE_MOVEMENT_DATE);
    }
  }

  private void validateAmountOrSnapshot() throws BusinessException {
    if (movementAmount == null && balanceSnapshot == null) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_AMOUNT);
    }
    validateAmountWithCategory();
  }

  private void validateCategoryRequirement() throws BusinessException {
    // FIXME Duplicated
    if (CategoryDomain.isEmpty(category) && movementType != MovementType.BALANCE_SNAPSHOT) {

      throw new BusinessException(BusinessDomainExceptionType.EMPTY_CATEGORY);
    }
  }

  private void validateCategoryByMovementType() throws BusinessException {
    switch (this.movementType) {
      case BALANCE_SNAPSHOT:
        validateBalanceSnapshotCategory();
        break;
      case DEPOSIT:
        validateDepositCategory();
        break;
      case WITHDRAWAL:
        validateWithdrawalCategory();
        break;
    }
  }

  private void validateAmountWithCategory() throws BusinessException {
    if (movementType == MovementType.DEPOSIT && movementAmount.signum() < 0) {
      throw new BusinessException(BusinessDomainExceptionType.DEPOSIT_AMOUNT_NOT_POSITIVE);
    }
    if (movementType == MovementType.WITHDRAWAL
        && (movementAmount == null || movementAmount.signum() > 0)) {
      throw new BusinessException(BusinessDomainExceptionType.WITHDRAWAL_AMOUNT_NOT_POSITIVE);
    }
  }

  private void validateBalanceSnapshotCategory() throws BusinessException {
    if (category != null) {
      throw new BusinessException(BusinessDomainExceptionType.INVALID_CATEGORY_BALANCE_SNAPSHOT);
    }
  }

  private void validateDepositCategory() {
    if (!category.isIncome()) {
      throw new GenericSpecificationException("Category must be income");
    }
  }

  private void validateWithdrawalCategory() {
    if (!category.isExpense()) {
      final String categoryMustBeExpense =
          String.format("Category %s must be expense", category.getType());
      throw new GenericSpecificationException(categoryMustBeExpense);
    }
  }

  public boolean hasMetadata(final MovementMetadataKey fieldName) {
    return metadata != null && metadata.hasKey(fieldName);
  }

  public Object getMetadataField(final MovementMetadataKey key) {
    return metadata != null ? metadata.get(key) : null;
  }

  public boolean isToCloseProduct() {
    return movementType.isWithdrawal()
        && category.isExpense()
        && category.isInvestmentWithdrawalToCloseIt();
  }

  @Override
  public String toString() {
    return "MovementDomain{"
        + "movementType="
        + movementType
        + ", movementAmount="
        + movementAmount
        + ", movementDate="
        + movementDate
        + ", balanceSnapshot="
        + balanceSnapshot
        + '}';
  }
}
