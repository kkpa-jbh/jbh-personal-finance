package com.jbh.accounts_mgmt.movements;

import com.jbh.accounts_mgmt.accounts.AccountId;
import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class AccountMovement {

  private AccountMovementId id;
  private AccountId accountId;
  private MovementType movementType;
  private BigDecimal movementAmount;
  private LocalDate movementDate;
  private BigDecimal balanceSnapshot;

  private AccountMovement(AccountId accountId, LocalDate movementDate, BigDecimal movementAmount,
      MovementType movementType) {
    this.accountId = accountId;
    this.movementDate = movementDate;
    this.movementAmount = movementAmount;
    this.movementType = movementType;
    validate();
  }

  public static AccountMovement of(AccountId accountId, LocalDate movementDate, BigDecimal totalAmount) {
    MovementType txnType =
        totalAmount.compareTo(BigDecimal.ZERO) >= 0 ? MovementType.DEPOSIT : MovementType.WITHDRAWAL;
    return new AccountMovement(accountId, movementDate, totalAmount, txnType);
  }

  public static AccountMovement of(AccountId accountId, LocalDate movementDate, BigDecimal totalAmount,
      BigDecimal balanceSnapshot) {
    MovementType txnType =
        totalAmount.compareTo(BigDecimal.ZERO) >= 0 ? MovementType.DEPOSIT : MovementType.WITHDRAWAL;
    return new AccountMovement(accountId, movementDate, totalAmount, txnType);
  }

  private void validate() {
    if (accountId == null || accountId.value() == null) {
      throw new GenericSpecificationException("Account ID cannot be null");
    }
    if (movementDate == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
    if (movementAmount == null) {
      throw new GenericSpecificationException("Total amount cannot be null");
    }

    if (movementDate.isAfter(LocalDate.now())) {
      throw new GenericSpecificationException("Movement date cannot be in the future");
    }
  }
}
