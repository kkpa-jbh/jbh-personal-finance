package com.jbh.finance.infra.adapters.in.rest.balancehistory.response;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.finance.application.feature.monthlybalance.dto.balancehistory.BalanceHistorySummaryDTO;
import java.math.BigDecimal;

/**
 * API response for balance history summary.
 *
 * @param totalBalance The current total balance at the end of the selected period. Sum of closing
 *     balances across all products for the latest month.
 * @param periodChange This is how much my balance grew (or shrank) during this period
 * @param periodChangePercent percentage change over the selected period
 * @param avgGrowthRate The average monthly growth rate across the period. On average, my balance
 *     grew by this percentage each month
 * @param totalMovements The total number of transactions/movements during the period
 */
public record BalanceHistorySummaryResponse(
    BigDecimal totalBalance,
    BigDecimal periodChange,
    BigDecimal periodChangePercent,
    BigDecimal avgGrowthRate,
    int totalMovements) {

  /**
   * Creates a response from the internal DTO.
   *
   * @param dto the internal balance history summary DTO
   * @return the API response
   */
  public static BalanceHistorySummaryResponse fromDTO(final BalanceHistorySummaryDTO dto) {
    return new BalanceHistorySummaryResponse(
        dto.totalBalance(),
        dto.periodChange(),
        dto.periodChangePercent(),
        dto.avgGrowthRate(),
        dto.totalMovements());
  }

  /**
   * Creates an empty summary response.
   *
   * @return empty summary response
   */
  public static BalanceHistorySummaryResponse empty() {
    return new BalanceHistorySummaryResponse(JBH_ZERO, JBH_ZERO, JBH_ZERO, JBH_ZERO, 0);
  }
}
