package com.jbh.finance.application.feature.monthlybalance.dto.balancehistory;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.application.feature.product.dto.ProductDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.YearMonth;

/**
 * Detailed month-by-month breakdown of all balance data.
 *
 * @param period
 * @param openingBalance
 * @param closingBalance
 * @param profit
 * @param growthRate
 * @param movementCount
 * @param isGapPeriod
 * @param isOfficialReport
 * @param productId
 * @param productName
 * @param isProfitable
 * @param isLoss
 */
public record BalanceHistoryEntryDTO(
    YearMonth period,
    BigDecimal openingBalance,
    BigDecimal closingBalance,
    BigDecimal profit,
    BigDecimal growthRate,
    int movementCount,
    boolean isGapPeriod,
    boolean isOfficialReport,
    ProductId productId,
    String productName,
    boolean isProfitable,
    boolean isLoss,
    BigDecimal totalDebits,
    BigDecimal totalCredits) {

  public static BalanceHistoryEntryDTO fromDTO(
      final MonthlyBalanceDTO monthlyBalance, final ProductDTO productDTO) {
    return new BalanceHistoryEntryDTO(
        monthlyBalance.period(),
        monthlyBalance.openingBalance(),
        monthlyBalance.closingBalance(),
        monthlyBalance.officialMonthlyReport()
            ? monthlyBalance.monthlyReportedProfit()
            : monthlyBalance.monthlyNetProfit(),
        monthlyBalance.netGrowthRate(),
        monthlyBalance.totalMovements(),
        monthlyBalance.gapPeriod(),
        monthlyBalance.officialMonthlyReport(),
        productDTO.id(),
        productDTO.name(),
        monthlyBalance.isProfitable(),
        monthlyBalance.isLoss(),
        monthlyBalance.totalDebits(),
        monthlyBalance.totalCredits());
  }
}
