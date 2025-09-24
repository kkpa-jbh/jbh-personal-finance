package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.isNotZero;
import static com.jbh.account.domain.utils.MoneyUtils.isZero;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** Domain entity - focus on business logic and state, not sorting */
@AllArgsConstructor
@Getter
@SuppressWarnings("PMD.ImmutableField")
public class AccountMonthlyBalanceDomain {

  private final Long id;
  private final AccountId accountId;
  private final Integer year;
  private final Integer month;
  private final YearMonth period;

  /** Growth = (Closing - Opening - NetFlows) / (Opening + 0.5 × NetFlows) */
  private BigDecimal netGrowthRate = JBH_ZERO;

  private BigDecimal totalDebits = JBH_ZERO; // Just for INFO purposes;
  private BigDecimal totalCredits = JBH_ZERO; // Just for INFO purposes;
  private BigDecimal movementBalance = JBH_ZERO; // NetFlow: Total Debits minus Total Credits;
  private BigDecimal openingBalance = JBH_ZERO; // Saldo inicial del mes
  private BigDecimal closingBalance = JBH_ZERO; // Saldo final del mes
  private BigDecimal monthlyProfit =
      JBH_ZERO; // Ganancia/perdida del mes. The contrary of Monthly expenses.
  private BigDecimal monthlyExpenses =
      JBH_ZERO; // This would represent how much you "consumed" from your previous balance
  private Integer totalMovements = 0; // Cantidad de movimientos en el mes.
  private boolean gapPeriod; // Month Balance was not registered in the past.
  private boolean officialMonthlyReport;
  private BigDecimal monthlyProfitReported;

  /** Constructor with required fields. */
  private AccountMonthlyBalanceDomain(final AccountId accountId, final YearMonth period) {
    this.accountId = accountId;
    this.period = period;
    this.year = period.getYear();
    this.month = period.getMonthValue();

    this.id = null;
  }

  public static AccountMonthlyBalanceDomain withPeriod(
      final AccountId accountId, final YearMonth period) {
    return new AccountMonthlyBalanceDomain(accountId, period);
  }

  // TODO: Remove this. It's only used for testing
  public static AccountMonthlyBalanceDomain withInitialDataForNextMonth(
      final AccountId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {

    final AccountMonthlyBalanceDomain accountMonthlyBalance =
        new AccountMonthlyBalanceDomain(accountId, period);
    accountMonthlyBalance.closingBalance = closingBalance;
    accountMonthlyBalance.gapPeriod = gapPeriod;

    return accountMonthlyBalance;
  }

  /** Returns a hash code value for the object based on period and accountId. */
  @Override
  public int hashCode() {
    return Objects.hash(accountId, period);
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

  public void adjustOpeningBalance(final AccountMonthlyBalanceDomain previousMonthlyBalance) {
    if (this.officialMonthlyReport) {
      throw new GenericSpecificationException(
          "Cannot adjust opening balance for an official monthly report");
    }
    final BigDecimal inputOpeningBalance = previousMonthlyBalance.getClosingBalance();
    if (inputOpeningBalance == null) {
      throw new GenericSpecificationException("Opening balance cannot be null");
    }
    this.openingBalance = withJBHDecimals(inputOpeningBalance);
  }

  public void assignOfficialMonthlyReport(
      final BigDecimal closingBalance, final BigDecimal monthlyProfitReported) {
    setOfficialMonthlyReport(closingBalance, monthlyProfitReported);
    syncPersistedBalance();
  }

  private void setOfficialMonthlyReport(
      final BigDecimal closingBalance, final BigDecimal monthlyProfitReported) {
    this.closingBalance = closingBalance != null ? withJBHDecimals(closingBalance) : JBH_ZERO;
    this.monthlyProfitReported =
        monthlyProfitReported != null ? withJBHDecimals(monthlyProfitReported) : JBH_ZERO;
    this.officialMonthlyReport = true;
  }

  /**
   * Syncs the balances from async tasks. This is called when syncing monthly balances
   * asynchronously and when syncing the current and next monthly balances. The monthly balances are
   * already persisted in the database.
   */
  public void syncPersistedBalance() {
    syncMonthlyProfit();
    syncMonthlyExpenses();
  }

  private void syncMonthlyProfit() {
    if (openingBalance == null) {
      throw new IllegalArgumentException(
          "The opening Balance is not set for " + this.getAccountId());
    }
    // I think it's for file upload
    if (isZero(totalCredits) && isZero(totalDebits) && isZero(openingBalance)) {
      return;
    }
    monthlyProfit = closingBalance.subtract(openingBalance).subtract(getMovementBalance());
  }

  private void syncMonthlyExpenses() {
    if (isNotZero(this.openingBalance)) {
      this.monthlyExpenses =
          this.movementBalance.add(this.openingBalance).subtract(this.closingBalance);
    } else {
      this.monthlyExpenses = JBH_ZERO;
    }
  }

  public BigDecimal getMovementBalance() {
    return totalDebits.subtract(totalCredits);
  }

  public void syncMovements(final List<AccountMovementDomain> movementsInPeriod) {
    movementsInPeriod.forEach(this::syncMovement);
  }

  public void syncMovement(final AccountMovementDomain movement) {

    validateMovementPeriod(movement);

    final BigDecimal amount = movement.getMovementAmount();

    if (isNotZero(amount)) {
      this.totalMovements++;
      syncIncomingAmount(amount);
    }

    if (movement.getBalanceSnapshot() != null) {
      this.closingBalance = movement.getBalanceSnapshot();
    }

    syncPersistedBalance();
  }

  private void validateMovementPeriod(final AccountMovementDomain mvmt) {
    if (mvmt.getMovementDate() == null) {
      throw new GenericSpecificationException(
          "Movement date cannot be null when syncing monthly balance");
    }
    final YearMonth movementPeriod = YearMonth.from(mvmt.getMovementDate());
    if (this.getPeriod().isBefore(movementPeriod) || this.getPeriod().isAfter(movementPeriod)) {
      throw new GenericSpecificationException(
          "Movement date is not in the same period as the monthly balance");
    }
  }

  private void syncIncomingAmount(final BigDecimal amount) {
    if (isNotZero(amount)) {
      if (amount.signum() < 0) {
        this.totalCredits = this.totalCredits.add(amount.abs());
      } else {
        this.totalDebits = this.totalDebits.add(amount.abs());
      }
      if (!this.officialMonthlyReport) {
        this.closingBalance = this.closingBalance.add(amount);
      }
    }
  }
}
