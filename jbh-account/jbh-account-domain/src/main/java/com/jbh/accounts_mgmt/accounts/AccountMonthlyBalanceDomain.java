package com.jbh.accounts_mgmt.accounts;

import com.jbh.accounts_mgmt.movements.AccountMovement;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AccountMonthlyBalanceDomain {

  private Long id;
  private AccountId accountId;
  private Integer balanceYear;
  private Integer balanceMonth;
  private BigDecimal openingBalance;
  private BigDecimal closingBalance;
  private BigDecimal totalCredits;
  private BigDecimal totalDebits;
  private Integer MovementCount;

  public static AccountMonthlyBalanceDomain of(AccountId accountId, int year, int month) {
    return new AccountMonthlyBalanceDomain(null, accountId, year, month,
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
  }


  public void syncMovement(AccountMovement txnDomain) {
    this.MovementCount++;

    BigDecimal amount = txnDomain.getMovementAmount();
    if (amount.signum() > 0) {
      this.totalCredits = this.totalCredits.add(amount);
    } else {
      this.totalDebits = this.totalDebits.add(amount.abs());
    }

    this.closingBalance = this.closingBalance.add(amount);

  }
}

