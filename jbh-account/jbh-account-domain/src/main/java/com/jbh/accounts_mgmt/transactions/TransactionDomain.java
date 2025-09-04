package com.jbh.accounts_mgmt.transactions;

import com.jbh.accounts_mgmt.accounts.domain.AccountId;
import com.jbh.accounts_mgmt.exceptions.GenericSpecificationException;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Data;

@Data
public class TransactionDomain {

  private TransactionId id;
  private AccountId accountId;
  private TransactionType txnType;
  private BigDecimal totalAmount;
  private LocalDate txnDate;

  private TransactionDomain(AccountId accountId, LocalDate txnDate, BigDecimal totalAmount, TransactionType txnType) {
    this.accountId = accountId;
    this.txnDate = txnDate;
    this.totalAmount = totalAmount;
    this.txnType = txnType;
    validate();
  }

  public static TransactionDomain of(AccountId accountId, LocalDate txnDate, BigDecimal totalAmount) {
    TransactionType txnType =
        totalAmount.compareTo(BigDecimal.ZERO) >= 0 ? TransactionType.DEPOSIT : TransactionType.WITHDRAWAL;
    return new TransactionDomain(accountId, txnDate, totalAmount, txnType);
  }

  private void validate() {
    if (accountId == null || accountId.value() == null) {
      throw new GenericSpecificationException("Account ID cannot be null");
    }
    if (txnDate == null) {
      throw new GenericSpecificationException("Transaction date cannot be null");
    }
    if (totalAmount == null) {
      throw new GenericSpecificationException("Total amount cannot be null");
    }

    if (txnDate.isAfter(LocalDate.now())) {
      throw new GenericSpecificationException("Transaction date cannot be in the future");
    }
  }
}
