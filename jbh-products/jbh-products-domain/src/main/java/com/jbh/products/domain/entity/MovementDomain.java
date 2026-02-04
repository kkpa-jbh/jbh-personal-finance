package com.jbh.products.domain.entity;

import static com.jbh.products.domain.entity.MovementCategoryDomain.withCategoryType;

import com.jbh.commons.exception.BusinessException;
import com.jbh.commons.exception.GenericSpecificationException;
import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.products.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.products.domain.vo.AccountMovementMetadata;
import com.jbh.products.domain.vo.AccountMovementMetadataKey;
import com.jbh.products.domain.vo.ExpenseCategory;
import com.jbh.products.domain.vo.IncomeCategory;
import com.jbh.products.domain.vo.MovementId;
import com.jbh.products.domain.vo.MovementType;
import com.jbh.products.domain.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@SuppressWarnings({"PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal", "PMD.GodClass"})
public class MovementDomain {

  private final MovementId id;
  private final ProductId accountId;
  private final MovementType movementType;
  private final MovementCategoryDomain category;

  /**
   * For withdrawals, the amount is negative. For deposits, the amount is positive. For balance
   * snapshots, the amount indicates the type of movement (deposit, withdrawal.)
   */
  private final BigDecimal movementAmount;

  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;
  private final AccountMovementMetadata metadata;
  private final String description;

  public MovementDomain(
      final MovementId id,
      final ProductId accountId,
      final MovementType movementType,
      final MovementCategoryDomain category,
      final BigDecimal movementAmount,
      final LocalDate movementDate,
      final BigDecimal balanceSnapshot,
      final AccountMovementMetadata metadata,
      final String description) {
    this.id = id;
    this.accountId = accountId;
    this.movementType = movementType;
    this.category = category;
    this.movementAmount = JbhMoneyUtils.withJBHDecimals(movementAmount);
    this.movementDate = movementDate;
    this.balanceSnapshot = JbhMoneyUtils.withJBHDecimals(balanceSnapshot);
    this.metadata = metadata;
    this.description = description;
  }

  public MovementDomain(
      final ProductId accountId,
      final MovementType movementType,
      final LocalDate movementDate,
      final BigDecimal movementAmount,
      final BigDecimal balanceSnapshot,
      final AccountMovementMetadata metadata,
      final MovementCategoryDomain category,
      final String description) {
    this(
        MovementId.generate(),
        accountId,
        movementType,
        category,
        movementAmount,
        movementDate,
        balanceSnapshot,
        metadata,
        description);
  }

  // FIXME Use factory movemtn type and see if this method can be removed
  public static MovementDomain withFileImport(
      final ProductId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final LocalDateTime importedAt)
      throws BusinessException {

    MovementCategoryDomain category = null;
    if (movementType == MovementType.DEPOSIT) {
      category = withCategoryType(IncomeCategory.OTHER);
    } else if (movementType == MovementType.WITHDRAWAL) {
      category = withCategoryType(ExpenseCategory.PERSONAL);
    }

    final MovementDomain movementDomain =
        new MovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            AccountMovementMetadata.createEmpty(),
            category,
            null);

    movementDomain.validate();

    movementDomain.getMetadata().putFileImportedAt(importedAt);

    return movementDomain;
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
    if (accountId == null || accountId.value() == null) {
      throw new GenericSpecificationException("Account ID cannot be null");
    }
  }

  private void validateMovementType() throws BusinessException {
    if (movementType == null) {
      throw new BusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENT_TYPE);
    }
    if (movementType == MovementType.BALANCE_SNAPSHOT && category != null) {
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
    if (MovementCategoryDomain.isEmpty(category) && movementType != MovementType.BALANCE_SNAPSHOT) {

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

  public boolean hasMetadata(final AccountMovementMetadataKey fieldName) {
    return metadata != null && metadata.hasKey(fieldName);
  }

  public Object getMetadataField(final AccountMovementMetadataKey key) {
    return metadata != null ? metadata.get(key) : null;
  }

  public boolean isToCloseProduct() {
    return movementType.isWithdrawal()
        && category.isExpense()
        && category.getType() == ExpenseCategory.INVESTMENT_WITHDRAWAL_TO_CLOSE_IT;
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
