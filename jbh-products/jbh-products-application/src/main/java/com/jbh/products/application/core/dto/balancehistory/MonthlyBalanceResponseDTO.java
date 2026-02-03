package com.jbh.products.application.core.dto.balancehistory;

import com.jbh.products.application.core.dto.MonthlyBalanceDTO;
import com.jbh.products.application.core.dto.ProductDTO;
import com.jbh.products.domain.vo.ProductId;
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
public record MonthlyBalanceResponseDTO(
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
    boolean isLoss) {

  public static MonthlyBalanceResponseDTO fromDTO(
      final MonthlyBalanceDTO monthlyBalance, final ProductDTO productDTO) {
    return new MonthlyBalanceResponseDTO(
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
        monthlyBalance.isLoss());
  }
}
