package com.jbh.account.infra.adapters.out.persistence.monthlybalance;

import com.jbh.account.application.accounts.dto.AccountMonthlyBalanceDTO;
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
public class AccountMonthlyBalanceJPAEntity extends PanacheEntityBase {

  @Id
  @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "monthly_balance_seq")
  @SequenceGenerator(
      name = "monthly_balance_seq",
      sequenceName = "account_monthly_balances_id_seq",
      allocationSize = 1)
  @Column(name = "id")
  public Long id;

  @Column(name = "account_id")
  public UUID accountId;

  @Column(name = "year")
  public Integer year;

  @Column(name = "month")
  public Integer month;

  @Column(name = "period")
  @Convert(converter = YearMonthConverter.class)
  public YearMonth period;

  @Column(name = "total_debits")
  public BigDecimal totalDebits;

  @Column(name = "total_credits")
  public BigDecimal totalCredits;

  @Column(name = "movement_balance")
  public BigDecimal movementBalance;

  @Column(name = "opening_balance")
  public BigDecimal openingBalance;

  @Column(name = "closing_balance")
  public BigDecimal closingBalance;

  @Column(name = "monthly_profit")
  public BigDecimal monthlyProfit;

  @Column(name = "total_movements")
  public Integer totalMovements;

  @Column(name = "gap_period")
  public boolean gapPeriod;

  @Column(name = "created_at", nullable = false, updatable = false)
  public LocalDateTime createdAt;

  @Column(name = "updated_at", nullable = false)
  public LocalDateTime updatedAt;

  public static AccountMonthlyBalanceJPAEntity of(final AccountMonthlyBalanceDTO monthlyBalance) {
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
    entity.setMonthlyProfit(monthlyBalance.monthlyProfit());
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

  public AccountMonthlyBalanceDTO toDTO() {
    return AccountMonthlyBalanceDTO.defaultBuilder()
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
        .monthlyProfit(monthlyProfit)
        .totalMovements(totalMovements)
        .gapPeriod(gapPeriod)
        .build();
  }
}
