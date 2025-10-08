package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.vo.AccountId;
import io.quarkus.hibernate.orm.panache.PanacheEntityBase;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(name = "account_monthly_balances", schema = "acctmgmt")
@SuppressWarnings("PMD.TooManyFields")
public class AccountMonthlyBalanceJPAEntity extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "monthly_balance_seq")
  @SequenceGenerator(
      name = "monthly_balance_seq",
      sequenceName = "account_monthly_balances_id_seq",
      allocationSize = 1)
  @Column(name = "id")
  private Long id;

  @Column(name = "account_id")
  private UUID accountId;

  @Column(name = "year")
  private Integer year;

  @Column(name = "month")
  private Integer month;

  @Column(name = "period")
  @Convert(converter = YearMonthConverter.class)
  private YearMonth period;

  @Column(name = "total_debits")
  private BigDecimal totalDebits;

  @Column(name = "total_credits")
  private BigDecimal totalCredits;

  @Column(name = "movement_balance")
  private BigDecimal movementBalance;

  @Column(name = "opening_balance")
  private BigDecimal openingBalance;

  @Column(name = "closing_balance")
  private BigDecimal closingBalance;

  @Column(name = "monthly_reported_profit")
  private BigDecimal monthlyReportedProfit;

  @Column(name = "monthly_net_profit")
  private BigDecimal monthlyNetProfit;

  @Column(name = "income_withholding_tax_amount")
  private BigDecimal incomeWithholdingTaxAmount;

  @Column(name = "net_growth_rate")
  private BigDecimal netGrowthRate;

  @Column(name = "total_movements")
  private Integer totalMovements;

  @Column(name = "gap_period")
  private boolean gapPeriod;

  @Column(name = "official_monthly_report")
  private boolean officialMonthlyReport;

  @Column(name = "created_at", nullable = false, updatable = false)
  private LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  private LocalDateTime updatedAt;

  public static AccountMonthlyBalanceJPAEntity of(final MonthlyBalanceDTO monthlyBalance) {
    final AccountMonthlyBalanceJPAEntity entity = new AccountMonthlyBalanceJPAEntity();
    entity.setId(monthlyBalance.id());
    entity.setAccountId(
        monthlyBalance.accountId() != null ? monthlyBalance.accountId().value() : null);
    entity.setYear(monthlyBalance.year());
    entity.setMonth(monthlyBalance.month());
    entity.setPeriod(monthlyBalance.period());
    entity.setTotalDebits(monthlyBalance.totalDebits());
    entity.setTotalCredits(monthlyBalance.totalCredits());
    entity.setMovementBalance(monthlyBalance.movementBalance());
    entity.setOpeningBalance(monthlyBalance.openingBalance());
    entity.setClosingBalance(monthlyBalance.closingBalance());
    entity.setMonthlyNetProfit(monthlyBalance.monthlyNetProfit());
    entity.setNetGrowthRate(monthlyBalance.netGrowthRate());
    entity.setOfficialMonthlyReport(monthlyBalance.officialMonthlyReport());
    entity.setMonthlyReportedProfit(monthlyBalance.monthlyProfitReported());
    entity.setIncomeWithholdingTaxAmount(monthlyBalance.incomeWithholdingTaxAmount());
    entity.setTotalMovements(monthlyBalance.totalMovements());
    entity.setGapPeriod(monthlyBalance.gapPeriod());
    return entity;
  }

  @PrePersist
  protected void onCreate() {
    createdAt = LocalDateTime.now();
    updatedAt = LocalDateTime.now();
  }

  @PreUpdate
  protected void onUpdate() {
    updatedAt = LocalDateTime.now();
  }

  public MonthlyBalanceDTO toDTO() {
    return MonthlyBalanceDTO.defaultBuilder()
        .id(id)
        .accountId(AccountId.of(accountId))
        .year(year)
        .month(month)
        .period(period)
        .totalDebits(totalDebits)
        .totalCredits(totalCredits)
        .movementBalance(movementBalance)
        .openingBalance(openingBalance)
        .closingBalance(closingBalance)
        .monthlyNetProfit(monthlyNetProfit)
        .netGrowthRate(netGrowthRate)
        .officialMonthlyReport(officialMonthlyReport)
        .monthlyProfitReported(monthlyReportedProfit)
        .incomeWithholdingTaxAmount(incomeWithholdingTaxAmount)
        .totalMovements(totalMovements)
        .gapPeriod(gapPeriod)
        .build();
  }
}
