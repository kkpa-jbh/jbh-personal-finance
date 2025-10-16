package com.jbh.account.application.core.dto;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;
import static com.jbh.account.domain.utils.MoneyUtils.withJBHDecimals;

import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.Builder;

@Builder(builderMethodName = "notUseThisInternalBuilder")
public record MonthlyBalanceDTO(
    Long id,
    AccountId accountId,
    int year,
    int month,
    YearMonth period,
    BigDecimal netGrowthRate,
    BigDecimal totalDebits,
    BigDecimal totalCredits,
    BigDecimal movementBalance,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal monthlyNetProfit,
    int totalMovements,
    boolean gapPeriod,
    boolean officialMonthlyReport,
    BigDecimal monthlyReportedProfit,
    BigDecimal incomeWithholdingTaxAmount,
    LocalDateTime createdAt,
    LocalDateTime updatedAt) {

  public static MonthlyBalanceDTO withInitialDataForNextMonth(
      final AccountId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {

    return defaultBuilder()
        .accountId(accountId)
        .period(period)
        .year(period.getYear())
        .month(period.getMonthValue())
        .closingBalance(closingBalance)
        .gapPeriod(gapPeriod)
        .build();
  }

  public static MonthlyBalanceDTO.MonthlyBalanceDTOBuilder defaultBuilder() {
    return notUseThisInternalBuilder()
        .movementBalance(JBH_ZERO)
        .closingBalance(JBH_ZERO)
        .totalDebits(JBH_ZERO)
        .totalCredits(JBH_ZERO)
        .monthlyNetProfit(JBH_ZERO)
        .totalMovements(0)
        .openingBalance(JBH_ZERO);
  }

  public static MonthlyBalanceDTO.MonthlyBalanceDTOBuilder withClosingBalance(
      final AccountId accountId, final YearMonth period, final BigDecimal closingBalance) {
    return defaultBuilder()
        .accountId(accountId)
        .period(period)
        .year(period.getYear())
        .month(period.getMonthValue())
        .closingBalance(withJBHDecimals(closingBalance));
  }
}
