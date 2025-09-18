package com.jbh.account.domain.entity;

import static com.jbh.account.domain.entity.MovementCategoryDomain.withCategoryType;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMovementId;
import com.jbh.account.domain.vo.CategorySource;
import com.jbh.account.domain.vo.ExpenseCategory;
import com.jbh.account.domain.vo.IncomeCategory;
import com.jbh.account.domain.vo.MovementType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@SuppressWarnings("PMD.ClassWithOnlyPrivateConstructorsShouldBeFinal")
public class AccountMovementDomain {

  public static final String FILE_IMPORT_TAG = "fileImport";
  public static final String FILE_IMPORTED_AT_TAG = "fileImportedAt";
  private final AccountMovementId id;
  private final AccountId accountId;
  private final MovementType movementType;
  private final MovementCategoryDomain category;

  /**
   * For withdrawals, the amount is negative. For deposits, the amount is positive. For balance
   * snapshots, the amount indicates the type of movement (deposit, withdrawal.)
   */
  private final BigDecimal movementAmount;

  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;
  private final Map<String, Object> metadata;

  private AccountMovementDomain(
      final AccountId accountId,
      final MovementType movementType,
      final LocalDate movementDate,
      final BigDecimal movementAmount,
      final BigDecimal balanceSnapshot,
      final Map<String, Object> metadata,
      final MovementCategoryDomain category) {
    this.id = AccountMovementId.generate();
    this.accountId = accountId;
    this.movementDate = movementDate;
    this.movementAmount = movementAmount;
    this.movementType = movementType;
    this.balanceSnapshot = balanceSnapshot;
    this.metadata = metadata;
    this.category = category;
  }

  public static AccountMovementDomain withFileImport(
      final AccountId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final LocalDateTime importedAt) {

    MovementCategoryDomain category = null;
    if (movementType == MovementType.DEPOSIT) {
      category = withCategoryType(IncomeCategory.OTHER);
    } else if (movementType == MovementType.WITHDRAWAL) {
      category = withCategoryType(ExpenseCategory.PERSONAL);
    }

    final AccountMovementDomain movementDomain =
        with(accountId, movementDate, totalAmount, balanceSnapshot, movementType, category);

    movementDomain.addMetadata(FILE_IMPORT_TAG, true);
    movementDomain.addMetadata(FILE_IMPORTED_AT_TAG, importedAt);
    return movementDomain;
  }

  public static AccountMovementDomain with(
      final AccountId accountId,
      final LocalDate movementDate,
      final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot,
      final MovementType movementType,
      final MovementCategoryDomain category) {

    final AccountMovementDomain movDomain =
        new AccountMovementDomain(
            accountId,
            movementType,
            movementDate,
            totalAmount,
            balanceSnapshot,
            new HashMap<>(),
            category);

    movDomain.validate();

    return movDomain;
  }

  private void addMetadata(final String key, final Object value) {
    metadata.put(key, value);
  }

  public void validate() {
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

  private void validateMovementType() {
    if (movementType == null) {
      throw new GenericSpecificationException("Movement type cannot be null");
    }
    if (movementType == MovementType.BALANCE_SNAPSHOT && category != null) {
      throw new GenericSpecificationException("Category cannot be provided for balance snapshots");
    }
  }

  private void validateMovementDate() {
    if (movementDate == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
  }

  private void validateCategory() {
    validateMovementType();
    validateCategoryRequirement();
    validateCategoryByMovementType();
  }

  private void validateMovementDateNotFuture() {
    if (movementDate != null && movementDate.isAfter(LocalDate.now())) {
      throw new GenericSpecificationException("Movement date cannot be in the future");
    }
  }

  private void validateAmountOrSnapshot() {
    if (movementAmount == null && balanceSnapshot == null) {
      throw new GenericSpecificationException("Total amount cannot be null");
    }
    validateAmountWithCategory();
  }

  private void validateCategoryRequirement() {
    if (MovementCategoryDomain.isEmpty(category) && movementType != MovementType.BALANCE_SNAPSHOT) {
      final String categoryCannotBeNull =
          "Category cannot be null for movement type " + movementType;
      throw new GenericSpecificationException(categoryCannotBeNull);
    }
  }

  private void validateCategoryByMovementType() {
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

  private void validateAmountWithCategory() {
    if (movementType == MovementType.DEPOSIT && movementAmount.signum() < 0) {
      throw new GenericSpecificationException("Deposit amount cannot be negative");
    }
    if (movementType == MovementType.WITHDRAWAL && movementAmount.signum() > 0) {
      throw new GenericSpecificationException("Withdrawal amount cannot be positive");
    }
  }

  private void validateBalanceSnapshotCategory() {
    if (category != null) {
      throw new GenericSpecificationException("Category cannot be provided for balance snapshots");
    }
  }

  private void validateDepositCategory() {
    if (category.getSource() != CategorySource.INCOME) {
      throw new GenericSpecificationException("Category must be income");
    }
  }

  private void validateWithdrawalCategory() {
    if (category.getSource() != CategorySource.EXPENSE) {
      final String categoryMustBeExpense =
          String.format("Category %s must be expense", category.getType());
      throw new GenericSpecificationException(categoryMustBeExpense);
    }
  }

  public boolean hasMetadata(final String fieldName) {
    return metadata != null && metadata.containsKey(fieldName);
  }

  public Object getMetadataField(final String key) {
    return metadata != null ? metadata.get(key) : null;
  }
}
