package com.jbh.accounts_mgmt.accounts;

import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AccountMonthlyBalanceDomain {

  private Long id;
  private AccountId accountId;
  private Integer balanceYear;
  private Integer balanceMonth;
  private YearMonth balancePeriod;
  private BigDecimal openingBalance; // Saldo inicial del mes
  private BigDecimal closingBalance; // Saldo final del mes
  private BigDecimal totalCredits;
  private BigDecimal totalDebits;
  private Integer totalMovements; // Cantidad de movimientos en el mes.

  public static AccountMonthlyBalanceDomain of(AccountId accountId, int year, int month) {
    return new AccountMonthlyBalanceDomain(null, accountId, year, month,
        YearMonth.of(year, month),
        BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0);
  }


  public void syncMovement(AccountMovementDomain mvmt) {
    BigDecimal amount = mvmt.getMovementAmount();

    if (amount != null) {
      this.totalMovements++;
      if (amount.signum() < 0) {
        this.totalCredits = this.totalCredits.add(amount.abs());
      } else {
        this.totalDebits = this.totalDebits.add(amount.abs());
      }
      this.closingBalance = this.closingBalance.add(amount);
    }

    if (mvmt.getBalanceSnapshot() != null) {
      this.closingBalance = mvmt.getBalanceSnapshot();
    }

  }
}

