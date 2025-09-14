package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.isNotZero;
import static com.jbh.account.domain.utils.MoneyUtils.isZero;

import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
@AllArgsConstructor
public class AccountMonthlyBalanceDomain implements Comparable<AccountMonthlyBalanceDomain> {

  private Long id;
  private AccountId accountId;
  private Integer year;
  private Integer month;
  private YearMonth period;

  private BigDecimal totalDebits;
  private BigDecimal totalCredits;
  private BigDecimal movementBalance;
  private BigDecimal openingBalance; // Saldo inicial del mes
  private BigDecimal closingBalance; // Saldo final del mes
  private BigDecimal monthlyProfit; // Ganancia/perdida del mes

  private Integer totalMovements; // Cantidad de movimientos en el mes.

  private boolean gapPeriod; // Month Balance was not registered in the past.

  public static AccountMonthlyBalanceDomain of(
      final AccountId accountId, final int year, final int month) {
    return new AccountMonthlyBalanceDomain(
        null,
        accountId,
        year,
        month,
        YearMonth.of(year, month),
        JBH_ZERO, // Total debits starts at 0
        JBH_ZERO, // Total credits starts at 0
        JBH_ZERO, // Total movmeents starts at 0
        JBH_ZERO, // Opening balance starts at 0
        JBH_ZERO, // Closing balance starts at 0
        JBH_ZERO, // Monthly profit starts at 0
        0,
        false);
  }

  public static AccountMonthlyBalanceDomain of(final AccountId accountId, final YearMonth period) {
    return new AccountMonthlyBalanceDomain(
        null,
        accountId,
        period.getYear(),
        period.getMonthValue(),
        period,
        JBH_ZERO, // Total debits starts at 0
        JBH_ZERO, // Total credits starts at 0
        JBH_ZERO, // Total movs starts at 0
        JBH_ZERO, // Opening balance starts at 0
        JBH_ZERO, // Closing balance starts at 0
        JBH_ZERO, // Monthly profit starts at 0
        0,
        false);
  }

  public static AccountMonthlyBalanceDomain of(
      final AccountId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {
    return new AccountMonthlyBalanceDomain(
        null,
        accountId,
        period.getYear(),
        period.getMonthValue(),
        period,
        JBH_ZERO, // Total debits starts at 0
        JBH_ZERO, // Total credits starts at 0
        JBH_ZERO, // Total movs starts at 0
        JBH_ZERO, // Opening balance starts at 0
        closingBalance,
        JBH_ZERO, // Monthly profit starts at 0
        0,
        gapPeriod);
  }

  public void syncMovements(final List<AccountMovementDomain> movementsInPeriod) {
    movementsInPeriod.forEach(this::syncMovement);
  }

  public void syncMovement(final AccountMovementDomain mvmt) {
    final BigDecimal amount = mvmt.getMovementAmount();

    if (amount != null && isNotZero(amount)) {
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

  public void refreshProfitMonthly() {
    if (this.getOpeningBalance() == null) {
      throw new IllegalArgumentException(
          "The opening Balance is not set for " + this.getAccountId());
    }
    if (isZero(totalCredits) && isZero(totalDebits) && isZero(openingBalance)) {
      return;
    }
    movementBalance = totalDebits.subtract(totalCredits);

    final BigDecimal result = totalDebits.subtract(totalCredits);
    this.monthlyProfit = closingBalance.subtract(openingBalance).subtract(result);
  }

  /**
   * Compares this account monthly balance with another based on the period (YearMonth). Natural
   * ordering is by period in ascending order (earliest period first).
   *
   * @param other the AccountMonthlyBalanceDomain to be compared
   * @return a negative integer, zero, or a positive integer as this period is before, equal to, or
   *     after the specified period
   * @throws NullPointerException if the specified object is null
   * @throws IllegalStateException if either this object's or the other object's period is null
   */
  @Override
  public int compareTo(final AccountMonthlyBalanceDomain other) {
    Objects.requireNonNull(other, "Cannot compare with null AccountMonthlyBalanceDomain");

    if (this.period == null) {
      throw new IllegalStateException("This object's period cannot be null for comparison");
    }

    if (other.period == null) {
      throw new IllegalStateException("Other object's period cannot be null for comparison");
    }

    return this.period.compareTo(other.period);
  }

  /** Returns a hash code value for the object based on period and accountId. */
  @Override
  public int hashCode() {
    return Objects.hash(period, accountId);
  }

  /**
   * Indicates whether some other object is "equal to" this one. Two AccountMonthlyBalanceDomain
   * objects are considered equal if they have the same period.
   */
  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final AccountMonthlyBalanceDomain that = (AccountMonthlyBalanceDomain) obj;
    return Objects.equals(period, that.period) && Objects.equals(accountId, that.accountId);
  }

  public AccountMonthlyBalanceDTO toDTO() {
    return AccountMonthlyBalanceDTO.builder()
        .id(id)
        .accountId(accountId)
        .year(year)
        .month(month)
        .period(period)
        .totalDebits(totalDebits)
        .totalCredits(totalCredits)
        .movementBalance(movementBalance)
        .openingBalance(openingBalance)
        .closingBalance(closingBalance)
        .monthlyProfit(monthlyProfit)
        .totalMovements(totalMovements)
        .gapPeriod(gapPeriod)
        .build();
  }
}
