package com.jbh.finance.application.feature.monthlybalance.dto.balancehistory;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import java.math.BigDecimal;

/**
 * @param totalBalance The current total balance at the end of the selected period. Sum of closing
 *     balances across all products for the latest month.
 * @param periodChange This is how much my balance grew (or shrank) during this period
 * @param periodChangePercent percentage change over the selected period
 * @param avgGrowthRate The average monthly growth rate across the period. On average, my balance
 *     grew by this percentage each month
 * @param totalMovements he total number of transactions/movements during the period
 */
public record BalanceHistorySummaryDTO(
    BigDecimal totalBalance,
    BigDecimal periodChange,
    BigDecimal periodChangePercent,
    BigDecimal avgGrowthRate,
    int totalMovements) {

  public static BalanceHistorySummaryDTO empty() {
    return new BalanceHistorySummaryDTO(JBH_ZERO, JBH_ZERO, JBH_ZERO, JBH_ZERO, 0);
  }
}
