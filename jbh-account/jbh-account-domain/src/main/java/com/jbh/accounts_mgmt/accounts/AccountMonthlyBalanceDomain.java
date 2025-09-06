package com.jbh.accounts_mgmt.accounts;

import static com.jbh.accounts_mgmt.utils.MoneyUtils.JBH_ZERO;

import com.jbh.accounts_mgmt.movements.AccountMovementDomain;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class AccountMonthlyBalanceDomain {


  private Long id;
  private AccountId accountId;
  private Integer year;
  private Integer month;
  private YearMonth period;

  private BigDecimal totalDebits;
  private BigDecimal totalCredits;
  private BigDecimal openingBalance; // Saldo inicial del mes
  private BigDecimal closingBalance; // Saldo final del mes
  private BigDecimal monthlyProfit; // Ganancia/perdida del mes

  private Integer totalMovements; // Cantidad de movimientos en el mes.

  public static AccountMonthlyBalanceDomain of(AccountId accountId, int year, int month) {
    return new AccountMonthlyBalanceDomain(null, accountId, year, month,
        YearMonth.of(year, month),
        JBH_ZERO, // Total debits starts at 0
        JBH_ZERO, // Total credits starts at 0
        JBH_ZERO,  // Opening balance starts at 0
        JBH_ZERO,  // Closing balance starts at 0
        JBH_ZERO, // Monthly profit starts at 0
        0);
  }


  public void syncMovement(AccountMovementDomain mvmt) {
    BigDecimal amount = mvmt.getMovementAmount();

    if (amount != null && amount.intValue() != BigDecimal.ZERO.intValue()) {
      this.totalMovements++;
      if (amount.signum() < 0) {
        this.totalCredits = this.totalCredits.add(amount.abs());
      } else {
        this.totalDebits = this.totalDebits.add(amount.abs());
      }
      this.closingBalance = this.closingBalance.add(amount);
    }

    if (mvmt.getBalanceSnapshot() != null) {

      amount = amount == null ? JBH_ZERO : amount;

      this.monthlyProfit = mvmt.getBalanceSnapshot().subtract(this.totalDebits.subtract(this.totalCredits));

      this.closingBalance = mvmt.getBalanceSnapshot();
    }

  }

  public void syncMovements(List<AccountMovementDomain> movementsInPeriod) {
    movementsInPeriod.forEach(this::syncMovement);
  }
}

