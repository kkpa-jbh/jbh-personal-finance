package com.jbh.accounts_mgmt.accounts.domain;

import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import com.jbh.accounts_mgmt.transactions.TransactionDomain;
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

  public void syncBalances(TransactionDomain txnDomain) {
    if (txnDomain == null) {
      throw new GenericSpecificationException("Transaction cannot be null");
    }
    if (txnDomain.getTxnDate() == null) {
      throw new GenericSpecificationException("Transaction date cannot be null");
    }
    if (txnDomain.getTotalAmount() == null) {
      throw new GenericSpecificationException("Transaction amount cannot be null");
    }

    if (txnDomain.getTotalAmount().signum() < 0) {
      if (this.effectiveBalance.subtract(txnDomain.getTotalAmount()).signum() < 0) {
        throw new GenericSpecificationException("Insufficient effective balance");
      }
    }
    applyTransaction(txnDomain);
  }

  private void applyTransaction(TransactionDomain txnDomain) {
    this.balance = this.balance.add(txnDomain.getTotalAmount());
    this.effectiveBalance = this.effectiveBalance.add(txnDomain.getTotalAmount());
  }
}
