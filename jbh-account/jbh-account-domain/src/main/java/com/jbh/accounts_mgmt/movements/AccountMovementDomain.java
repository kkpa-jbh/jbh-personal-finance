package com.jbh.accounts_mgmt.movements;

import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AccountMovementDomain {

  private AccountMovementId id;
  private AccountId accountId;
  private MovementType movementType;
  private BigDecimal movementAmount;
  private LocalDate movementDate;
  private BigDecimal balanceSnapshot;

  private AccountMovementDomain(AccountId accountId,
      MovementType movementType,
      LocalDate movementDate,
      BigDecimal movementAmount,
      BigDecimal balanceSnapshot
  ) {
    this.accountId = accountId;
    this.movementDate = movementDate;
    this.movementAmount = movementAmount;
    this.movementType = movementType;
    this.balanceSnapshot = balanceSnapshot;
    validate();
  }

  private static MovementType findMovementTypeBaseOnAmounts(BigDecimal totalAmount, BigDecimal balanceSnapshot) {
    MovementType movementType = null;

    if (totalAmount != null) {
      movementType = totalAmount.compareTo(BigDecimal.ZERO) >= 0 ? MovementType.DEPOSIT : MovementType.WITHDRAWAL;
    } else if (balanceSnapshot != null) {
      movementType = MovementType.BALANCE_SNAPSHOT;
    }
    return movementType;

  }

  public static AccountMovementDomain of(AccountId accountId, LocalDate movementDate, BigDecimal totalAmount) {
    MovementType movementType = findMovementTypeBaseOnAmounts(totalAmount, null);
    return new AccountMovementDomain(accountId, movementType, movementDate, totalAmount, null);
  }

  public static AccountMovementDomain of(AccountId accountId, LocalDate movementDate, BigDecimal totalAmount,
      BigDecimal balanceSnapshot) {
    MovementType movementType = findMovementTypeBaseOnAmounts(totalAmount, balanceSnapshot);
    return new AccountMovementDomain(accountId, movementType, movementDate, totalAmount, balanceSnapshot);
  }

  private void validate() {
    if (accountId == null || accountId.value() == null) {
      throw new GenericSpecificationException("Account ID cannot be null");
    }
    if (movementType == null) {
      throw new GenericSpecificationException("Movement type cannot be null");
    }
    if (movementDate == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
    if (movementDate.isAfter(LocalDate.now())) {
      throw new GenericSpecificationException("Movement date cannot be in the future");
    }
    if (movementAmount == null && balanceSnapshot == null) {
      throw new GenericSpecificationException("Total amount cannot be null");
    }
  }
}
