package com.jbh.accounts_mgmt.accounts;

import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import com.jbh.accounts_mgmt.movements.AccountMovement;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class AccountDomain {

  private AccountId id;
  private String name;
  private Long userId;
  private BigDecimal balance;
  private BigDecimal effectiveBalance;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  private AccountDomain() {
    this.balance = BigDecimal.ZERO;
    this.effectiveBalance = BigDecimal.ZERO;
    this.createdAt = LocalDateTime.now();
    this.updatedAt = LocalDateTime.now();
  }

  public static AccountDomain withId(AccountId id) {
    AccountDomain account = new AccountDomain();
    account.setId(id);
    return account;
  }

  public void syncBalances(AccountMovement txnDomain) {
    if (txnDomain == null) {
      throw new GenericSpecificationException("Movement cannot be null");
    }
    if (txnDomain.getMovementDate() == null) {
      throw new GenericSpecificationException("Movement date cannot be null");
    }
    if (txnDomain.getMovementAmount() == null) {
      throw new GenericSpecificationException("Movement amount cannot be null");
    }

    if (txnDomain.getMovementAmount().signum() < 0) {
      if (this.effectiveBalance.add(txnDomain.getMovementAmount()).signum() < 0) {
        throw new GenericSpecificationException("Insufficient effective balance");
      }
    }
    applyMovement(txnDomain);
  }

  private void applyMovement(AccountMovement txnDomain) {
    this.balance = this.balance.add(txnDomain.getMovementAmount());
    this.effectiveBalance = this.effectiveBalance.add(txnDomain.getMovementAmount());
  }
}
