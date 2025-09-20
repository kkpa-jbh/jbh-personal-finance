package com.jbh.account.application.accounts.dto;

import static com.jbh.account.domain.utils.MoneyUtils.JBH_ZERO;

import com.jbh.account.domain.vo.AccountId;
import java.math.BigDecimal;
import java.time.YearMonth;
import lombok.Builder;

@Builder
public record AccountMonthlyBalanceDTO(
    Long id,
    AccountId accountId,
    int year,
    int month,
    YearMonth period,
    BigDecimal estimatedAnnualYield,
    BigDecimal totalDebits,
    BigDecimal totalCredits,
    BigDecimal movementBalance,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal monthlyProfit,
    BigDecimal monthlyExpenses,
    int totalMovements,
    boolean gapPeriod,
    boolean officialMonthlyReport) {

  public static AccountMonthlyBalanceDTO.AccountMonthlyBalanceDTOBuilder defaultBuilder() {
    return AccountMonthlyBalanceDTO.builder()
        .movementBalance(JBH_ZERO)
        .closingBalance(JBH_ZERO)
        .totalDebits(JBH_ZERO)
        .totalCredits(JBH_ZERO)
        .monthlyProfit(JBH_ZERO)
        .monthlyExpenses(JBH_ZERO)
        .totalMovements(0)
        .openingBalance(JBH_ZERO);
  }
}
