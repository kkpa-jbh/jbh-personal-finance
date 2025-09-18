package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.isNotZero;
import static com.jbh.account.domain.utils.MoneyUtils.isZero;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;

import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import com.jbh.account.domain.vo.AccountMonthlyBalanceDTO;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/** Domain entity - focus on business logic and state, not sorting */
@Getter
@SuperBuilder
@AllArgsConstructor
@SuppressWarnings("PMD.ImmutableField")
public class AccountMonthlyBalanceDomain {

  private final Long id;
  private final AccountId accountId;
  private final Integer year;
  private final Integer month;
  private final YearMonth period;

  /**
   * Yield = Actual return earned (includes compounding effects) Compound Interest = Interest
   * compuesto YIELD is more appropriate since you're calculating actual returns based on monthly
   * performance
   */
  private BigDecimal estimatedAnnualYield = JBH_ZERO;

  private BigDecimal totalDebits = JBH_ZERO; // Just for INFO purposes;
  private BigDecimal totalCredits = JBH_ZERO; // Just for INFO purposes;
  private BigDecimal movementBalance = JBH_ZERO; // Total Debits minus Total Credits;
  private BigDecimal openingBalance = JBH_ZERO; // Saldo inicial del mes
  private BigDecimal closingBalance = JBH_ZERO; // Saldo final del mes
  private BigDecimal monthlyProfit = JBH_ZERO; // Ganancia/perdida del mes
  private BigDecimal monthlyExpenses =
      JBH_ZERO; // This would represent how much you "consumed" from your previous balance
  private Integer totalMovements = 0; // Cantidad de movimientos en el mes.
  private boolean gapPeriod; // Month Balance was not registered in the past.
  private boolean officialMonthlyReport;

  /** Constructor with required fields. */
  private AccountMonthlyBalanceDomain(final AccountId accountId, final YearMonth period) {
    this.accountId = accountId;
    this.period = period;
    this.year = period.getYear();
    this.month = period.getMonthValue();

    this.id = null;
  }

  public static AccountMonthlyBalanceDomain withPeriod(
      final AccountId accountId, final int year, final int month) {
    return new AccountMonthlyBalanceDomain(accountId, YearMonth.of(year, month));
  }

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
        .monthlyExpenses(monthlyExpenses)
        .totalMovements(totalMovements)
        .gapPeriod(gapPeriod)
        .estimatedAnnualYield(estimatedAnnualYield)
        .officialMonthlyReport(officialMonthlyReport)
        .build();
  }

  public void adjustOpeningBalance(final BigDecimal inputOpeningBalance) {
    validateOpeningBalanceUseCase(inputOpeningBalance);
    this.openingBalance = withJBHDecimals(inputOpeningBalance);
    this.officialMonthlyReport = false;
  }

  private static void validateOpeningBalanceUseCase(final BigDecimal inputOpeningBalance) {
    if (inputOpeningBalance == null) {
      throw new GenericSpecificationException("Opening balance cannot be null");
    }
  }

  public void syncOfficialMonthlyReport(
      final BigDecimal closingBalance, final BigDecimal monthlyProfitReported) {
    setOfficialMonthlyReport(closingBalance, monthlyProfitReported);
    syncMonthlyExpenses();
  }

  private void setOfficialMonthlyReport(
      final BigDecimal closingBalance, final BigDecimal monthlyProfitReported) {
    this.closingBalance = closingBalance != null ? withJBHDecimals(closingBalance) : JBH_ZERO;
    this.monthlyProfit =
        monthlyProfitReported != null ? withJBHDecimals(monthlyProfitReported) : JBH_ZERO;
    this.officialMonthlyReport = true;
  }

  private void syncMonthlyExpenses() {
    if (this.openingBalance != null) {
      this.monthlyExpenses =
          this.movementBalance.add(this.openingBalance).subtract(this.closingBalance);
    } else {
      this.monthlyExpenses = JBH_ZERO;
    }
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

    syncMovementBalanceAndMonthlyProfit();
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
      this.closingBalance = this.closingBalance.add(amount);
    }
  }

  public void syncMovementBalanceAndMonthlyProfit() {
    if (this.getOpeningBalance() == null) {
      throw new IllegalArgumentException(
          "The opening Balance is not set for " + this.getAccountId());
    }
    if (isZero(totalCredits) && isZero(totalDebits) && isZero(openingBalance)) {
      return;
    }
    // FIXME this is not the right place to do this
    syncMovementBalance();
    this.monthlyProfit = closingBalance.subtract(openingBalance).subtract(movementBalance);
  }

  private void syncMovementBalance() {
    this.movementBalance = totalDebits.subtract(totalCredits);
  }
}
