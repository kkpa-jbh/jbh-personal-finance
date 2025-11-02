package com.jbh.account.domain.entity;

import static com.jbh.account.domain.utils.JbhMoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isNotZero;
import static com.jbh.account.domain.utils.JbhMoneyUtils.isZero;
import static com.jbh.account.domain.utils.JbhMoneyUtils.withJBHDecimals;

import com.jbh.account.domain.calculators.MoneyGrowthCalculator;
import com.jbh.account.domain.exceptions.AccountBusinessException;
import com.jbh.account.domain.exceptions.BusinessDomainExceptionType;
import com.jbh.account.domain.exceptions.GenericSpecificationException;
import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.Objects;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Domain entity - focus on business logic and state, not sorting */
@Getter
@SuppressWarnings({
  "PMD.ImmutableField",
  "PMD.GodClass",
  "PMD.CyclomaticComplexity",
  "PMD.NPathComplexity"
})
public class AccountMonthlyBalanceDomain {
  private static final Logger LOG = LoggerFactory.getLogger(AccountMonthlyBalanceDomain.class);
  private final MoneyGrowthCalculator moneyGrowthCalculator = new MoneyGrowthCalculator();

  // Attributes
  private final Long id;
  private final AccountId accountId;
  private final Integer year;
  private final Integer month;
  private final YearMonth period;

  /** Growth = (Closing - Opening - NetFlows) / (Opening + 0.5 × NetFlows) */
  private BigDecimal netGrowthRate = JBH_ZERO;

  private BigDecimal totalDebits = JBH_ZERO; // Just for INFO purposes;
  private BigDecimal totalCredits = JBH_ZERO; // Just for INFO purposes;
  private BigDecimal openingBalance = JBH_ZERO; // Saldo inicial del mes
  private BigDecimal closingBalance = JBH_ZERO; // Saldo final del mes
  private BigDecimal monthlyNetProfit =
      JBH_ZERO; // Ganancia/perdida del mes. Positive for profit, negative for loss.

  private Integer totalMovements = 0; // Cantidad de movimientos en el mes.
  private boolean gapPeriod; // Month Balance was not registered in the past.
  private boolean officialMonthlyReport;

  // NULL if not reported
  private BigDecimal monthlyProfitReported;

  private BigDecimal incomeWithholdingTaxAmount;

  @SuppressWarnings({"PMD.ExcessiveParameterList", "PMD.NPathComplexity"})
  public AccountMonthlyBalanceDomain(
      final Long id,
      final AccountId accountId,
      final Integer year,
      final Integer month,
      final YearMonth period,
      final BigDecimal netGrowthRate,
      final BigDecimal totalDebits,
      final BigDecimal totalCredits,
      final BigDecimal openingBalance,
      final BigDecimal closingBalance,
      final BigDecimal monthlyNetProfit,
      final Integer totalMovements,
      final boolean gapPeriod,
      final boolean officialMonthlyReport,
      final BigDecimal monthlyProfitReported,
      final BigDecimal incomeWithholdingTaxAmount) {

    this.id = id;
    this.accountId = accountId;
    this.year = year;
    this.month = month;
    this.period = period;
    this.netGrowthRate = netGrowthRate != null ? netGrowthRate : JBH_ZERO;
    this.totalDebits = totalDebits != null ? totalDebits : JBH_ZERO;
    this.totalCredits = totalCredits != null ? totalCredits : JBH_ZERO;
    this.openingBalance = openingBalance != null ? openingBalance : JBH_ZERO;
    this.closingBalance = closingBalance != null ? closingBalance : JBH_ZERO;
    this.monthlyNetProfit = monthlyNetProfit != null ? monthlyNetProfit : JBH_ZERO;
    this.totalMovements = totalMovements != null ? totalMovements : 0;
    this.gapPeriod = gapPeriod;
    this.officialMonthlyReport = officialMonthlyReport;
    this.monthlyProfitReported = withJBHDecimals(monthlyProfitReported);
    this.incomeWithholdingTaxAmount = withJBHDecimals(incomeWithholdingTaxAmount);
  }

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

  public void assignOpeningBalance(final AccountMonthlyBalanceDomain previousMonthlyBalance) {
    if (this.officialMonthlyReport && previousMonthlyBalance.officialMonthlyReport) {
      LOG.info(
          "Skipping opening balance adjustment for an official monthly report {}", this.period);
      return;
    }
    if (this.officialMonthlyReport) {
      throw new GenericSpecificationException(
          "Cannot adjust opening balance for an official monthly report");
    }
    final BigDecimal inputOpeningBalance = previousMonthlyBalance.getClosingBalance();
    if (inputOpeningBalance == null) {
      throw new GenericSpecificationException("Opening balance cannot be null");
    }

    // final var previousProfitReported = previousMonthlyBalance.getMonthlyProfitReported();
    // TODO Review if it's correct to adjust the opening balance for a profit report
    // final boolean wasProfitReported =
    // previousMonthlyBalance.isOfficialMonthlyReport() && previousProfitReported != null;
    this.openingBalance = withJBHDecimals(inputOpeningBalance);

    LOG.info("Adjusted Opening Balance {} for next period: {}", this.openingBalance, this.period);
  }

  public void assignMovement(final AccountMovementDomain movement) throws AccountBusinessException {
    validateMovementPeriod(movement);
    syncBalancesByMovement(movement);
    recalculateBalances();
  }

  private void validateMovementPeriod(final AccountMovementDomain mvmt)
      throws AccountBusinessException {
    if (mvmt.getMovementDate() == null) {
      throw new AccountBusinessException(BusinessDomainExceptionType.EMPTY_MOVEMENT_DATE);
    }
    final YearMonth movementPeriod = YearMonth.from(mvmt.getMovementDate());
    if (this.getPeriod().isBefore(movementPeriod) || this.getPeriod().isAfter(movementPeriod)) {
      throw new AccountBusinessException(
          BusinessDomainExceptionType.INVALID_MOV_DATE_MONTHLY_PERIOD);
    }
  }

  private void syncBalancesByMovement(final AccountMovementDomain movement) {
    final BigDecimal amount = movement.getMovementAmount();
    if (isNotZero(amount)) {
      this.totalMovements++;
      if (amount.signum() < 0) {
        this.totalCredits = this.totalCredits.add(amount.abs());
      } else {
        this.totalDebits = this.totalDebits.add(amount.abs());
      }
      if (!this.officialMonthlyReport) {
        this.closingBalance = this.closingBalance.add(amount);
      }
    }
    // Do not update closing balance after adding a movement when it's an official report
    if (!this.officialMonthlyReport && movement.getBalanceSnapshot() != null) {
      this.closingBalance = movement.getBalanceSnapshot();
    }
  }

  /**
   * Syncs the balances from async tasks. This is called when syncing monthly balances
   * asynchronously and when syncing the current and next monthly balances. The monthly balances are
   * already persisted in the database.
   */
  public void recalculateBalances() throws AccountBusinessException {
    syncMonthlyNetProfit();
    syncNetGrowthRate();
  }

  private void syncMonthlyNetProfit() {
    if (openingBalance == null) {
      throw new IllegalArgumentException(
          "The opening Balance is not set for " + this.getAccountId());
    }
    // I think it's for file upload
    if (isZero(totalCredits) && isZero(totalDebits) && isZero(openingBalance)) {
      return;
    }

    // if it's not reported, or it was reported without a profit, calculate the monthly net profit
    if (!this.officialMonthlyReport || isZero(monthlyProfitReported)) {
      final var movementBalance = getMovementBalance();
      // FIXME: For Credit Cards, it's not possible to calculate the monthly net profit
      monthlyNetProfit = closingBalance.subtract(openingBalance).subtract(movementBalance);
    }
  }

  private void syncNetGrowthRate() throws AccountBusinessException {
    LOG.debug("Syncing Net Growth Rate for account {} and period {}", accountId, period);
    final var movementBalance = getMovementBalance();
    if (this.officialMonthlyReport && isNotZero(monthlyProfitReported)) {
      this.netGrowthRate =
          moneyGrowthCalculator.calculateGrowth(
              openingBalance, closingBalance.add(monthlyProfitReported), movementBalance);
    } else {
      this.netGrowthRate =
          moneyGrowthCalculator.calculateGrowth(openingBalance, closingBalance, movementBalance);
    }
  }

  /**
   * NetFlow: Total Debits minus Total Credits;
   *
   * @return
   */
  public BigDecimal getMovementBalance() {
    return totalDebits.subtract(totalCredits);
  }

  public void assignOfficialMonthlyReport(
      final BigDecimal closingBalance,
      final BigDecimal monthlyProfitReported,
      final BigDecimal incomeWithholdingTaxAmount)
      throws AccountBusinessException {
    setOfficialMonthlyReport(closingBalance, monthlyProfitReported, incomeWithholdingTaxAmount);
    recalculateBalances();
  }

  @SuppressWarnings("PMD.NullAssignment")
  private void setOfficialMonthlyReport(
      final BigDecimal closingBalance,
      final BigDecimal inputMonthlyProfitReported,
      final BigDecimal inputWithholdingTaxAmount) {
    this.officialMonthlyReport = true;

    this.closingBalance = closingBalance != null ? withJBHDecimals(closingBalance) : JBH_ZERO;

    this.monthlyProfitReported =
        inputMonthlyProfitReported != null ? withJBHDecimals(inputMonthlyProfitReported) : JBH_ZERO;

    // Negative
    this.incomeWithholdingTaxAmount =
        inputWithholdingTaxAmount != null ? withJBHDecimals(inputWithholdingTaxAmount) : JBH_ZERO;

    // If it's not reported, it's the same as the monthly net profit calculated
    this.monthlyNetProfit =
        inputMonthlyProfitReported != null
            ? monthlyProfitReported.add(incomeWithholdingTaxAmount.negate())
            : monthlyNetProfit;
  }
}
