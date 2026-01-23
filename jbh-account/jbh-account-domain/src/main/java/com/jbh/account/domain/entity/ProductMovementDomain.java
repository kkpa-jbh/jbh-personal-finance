package com.jbh.account.domain.entity;

import static com.jbh.account.domain.entity.MovementCategoryDomain.withCategoryType;

import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.exceptions.ProductBusinessException;
import com.jbh.account.domain.utils.JbhMoneyUtils;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.AccountMovementMetadata;
import com.jbh.account.domain.vo.AccountMovementMetadataKey;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementType;
import com.jbh.account.domain.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Getter;

@Getter
@SuppressWarnings({"PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal", "PMD.GodClass"})
public class ProductMovementDomain {

  private final AccountMovementId id;
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

  public ProductMovementDomain(
      final AccountMovementId id,
      final ProductId accountId,
      final MovementType movementType,
      final MovementCategoryDomain category,
      final BigDecimal movementAmount,
      final LocalDate movementDate,
      final BigDecimal balanceSnapshot,
      final AccountMovementMetadata metadata) {
    this.id = id;
    this.accountId = accountId;
    this.movementType = movementType;
    this.category = category;
    this.movementAmount = JbhMoneyUtils.withJBHDecimals(movementAmount);
    this.movementDate = movementDate;
    this.balanceSnapshot = JbhMoneyUtils.withJBHDecimals(balanceSnapshot);
    this.metadata = metadata;
  }

  public ProductMovementDomain(
      final ProductId accountId,
      final MovementType movementType,
      final LocalDate movementDate,
      final BigDecimal movementAmount,
      final BigDecimal balanceSnapshot,
      final AccountMovementMetadata metadata,
      final MovementCategoryDomain category) {
    this.id = AccountMovementId.generate();
    this.accountId = accountId;
    this.movementDate = movementDate;
    this.movementAmount = JbhMoneyUtils.withJBHDecimals(movementAmount);
    this.movementType = movementType;
    this.balanceSnapshot = JbhMoneyUtils.withJBHDecimals(balanceSnapshot);
    this.metadata = metadata;
    this.category = category;
  }

  // FIXME Use factory movemtn type and see if this method can be removed
  public static ProductMovementDomain withFileImport(
      final ProductId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final LocalDateTime importedAt)
      throws ProductBusinessException {

    MovementCategoryDomain category = null;
    if (movementType == MovementType.DEPOSIT) {
      category = withCategoryType(IncomeCategory.OTHER);
    } else if (movementType == MovementType.WITHDRAWAL) {
      category = withCategoryType(ExpenseCategory.PERSONAL);
    }

    final ProductMovementDomain movementDomain =
        new ProductMovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            AccountMovementMetadata.createEmpty(),
            category);

    movementDomain.validate();

    movementDomain.getMetadata().putFileImportedAt(importedAt);

    return movementDomain;
  }

  public void validate() throws ProductBusinessException {
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

  private void validateMovementType() throws ProductBusinessException {
    if (movementType == null) {
      throw new ProductBusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENT_TYPE);
    }
    if (movementType == MovementType.BALANCE_SNAPSHOT && category != null) {
      throw new ProductBusinessException(
          BusinessDomainExceptionType.INVALID_CATEGORY_BALANCE_SNAPSHOT);
    }
  }

  private void validateMovementDate() throws ProductBusinessException {
    if (movementDate == null) {
      throw new ProductBusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENT_TYPE);
    }
  }

  private void validateCategory() throws ProductBusinessException {
    validateMovementType();
    validateCategoryRequirement();
    validateCategoryByMovementType();
  }

  private void validateMovementDateNotFuture() throws ProductBusinessException {
    if (movementDate != null && movementDate.isAfter(LocalDate.now())) {
      throw new ProductBusinessException(BusinessDomainExceptionType.FUTURE_MOVEMENT_DATE);
    }
  }

  private void validateAmountOrSnapshot() throws ProductBusinessException {
    if (movementAmount == null && balanceSnapshot == null) {
      throw new ProductBusinessException(BusinessDomainExceptionType.EMPTY_AMOUNT);
    }
    validateAmountWithCategory();
  }

  private void validateCategoryRequirement() throws ProductBusinessException {
    if (MovementCategoryDomain.isEmpty(category) && movementType != MovementType.BALANCE_SNAPSHOT) {

      throw new ProductBusinessException(BusinessDomainExceptionType.EMPTY_CATEGORY);
    }
  }

  private void validateCategoryByMovementType() throws ProductBusinessException {
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

  private void validateAmountWithCategory() throws ProductBusinessException {
    if (movementType == MovementType.DEPOSIT && movementAmount.signum() < 0) {
      throw new ProductBusinessException(BusinessDomainExceptionType.DEPOSIT_AMOUNT_NOT_POSITIVE);
    }
    if (movementType == MovementType.WITHDRAWAL
        && (movementAmount == null || movementAmount.signum() > 0)) {
      throw new ProductBusinessException(
          BusinessDomainExceptionType.WITHDRAWAL_AMOUNT_NOT_POSITIVE);
    }
  }

  private void validateBalanceSnapshotCategory() throws ProductBusinessException {
    if (category != null) {
      throw new ProductBusinessException(
          BusinessDomainExceptionType.INVALID_CATEGORY_BALANCE_SNAPSHOT);
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
