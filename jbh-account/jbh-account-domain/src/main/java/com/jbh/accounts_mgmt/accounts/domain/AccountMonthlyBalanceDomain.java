package com.jbh.accounts_mgmt.accounts.domain;

import com.jbh.accounts_mgmt.transactions.TransactionDomain;
import java.math.BigDecimal;
import lombok.Builder;

@Builder
public class AccountMonthlyBalanceDomain {

  private Long id;
  private AccountId accountId;
  private Integer balanceYear;
  private Integer balanceMonth;
  private BigDecimal openingBalance;
  private BigDecimal closingBalance;
  private BigDecimal totalCredits;
  private BigDecimal totalDebits;
  private Integer transactionCount;

  public static AccountMonthlyBalanceDomain of(AccountId accountId, int year, int month) {
    return new AccountMonthlyBalanceDomain(null, accountId, year, month,
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
  }


  public void syncTransaction(TransactionDomain txnDomain) {
    this.transactionCount++;

    BigDecimal amount = txnDomain.getTotalAmount();
    if (amount.signum() > 0) {
      this.totalCredits = this.totalCredits.add(amount);
    } else {
      this.totalDebits = this.totalDebits.add(amount.abs());
    }

    this.closingBalance = this.closingBalance.add(amount);

  }
}

