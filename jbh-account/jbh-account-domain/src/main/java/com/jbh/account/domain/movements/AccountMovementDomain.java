package com.jbh.account.domain.movements;

import com.jbh.account.domain.accounts.AccountId;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public final class AccountMovementDomain {

  private final AccountMovementId id;
  private final AccountId accountId;
  private final MovementType movementType;
  private final BigDecimal movementAmount;
  private final LocalDate movementDate;
  private final BigDecimal balanceSnapshot;

  private AccountMovementDomain(final AccountId accountId,
      final MovementType movementType,
      final LocalDate movementDate,
      final BigDecimal movementAmount,
      final BigDecimal balanceSnapshot
  ) {
    this.id = AccountMovementId.generate();
    this.accountId = accountId;
    this.movementDate = movementDate;
    this.movementAmount = movementAmount;
    this.movementType = movementType;
    this.balanceSnapshot = balanceSnapshot;
    validate();
  }

  private static MovementType findMovementTypeBaseOnAmounts(
      final BigDecimal totalAmount, final BigDecimal balanceSnapshot) {
    MovementType movementType = null;

    if (totalAmount != null) {
      movementType = totalAmount.compareTo(BigDecimal.ZERO) >= 0 ? MovementType.DEPOSIT : MovementType.WITHDRAWAL;
    } else if (balanceSnapshot != null) {
      movementType = MovementType.BALANCE_SNAPSHOT;
    }
    return movementType;

  }

  public static AccountMovementDomain of(final AccountId accountId,
      final LocalDate movementDate, final
      BigDecimal totalAmount) {
    final MovementType movementType = findMovementTypeBaseOnAmounts(totalAmount, null);
    return new AccountMovementDomain(accountId, movementType, movementDate, totalAmount, null);
  }

  public static AccountMovementDomain of(final AccountId accountId,
      final LocalDate movementDate, final BigDecimal totalAmount,
      final BigDecimal balanceSnapshot) {
    final MovementType movementType = findMovementTypeBaseOnAmounts(totalAmount, balanceSnapshot);
    return new AccountMovementDomain(accountId, movementType, movementDate, totalAmount, balanceSnapshot);
  }
  
  public void validate() {
    validateAccountId();
    validateMovementType();
    validateMovementDate();
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
  }

  private void validateMovementDate() {
    if (movementDate == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
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
  }

}
