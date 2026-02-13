package com.jbh.finance.application.feature.monthlybalance.dto;

import static com.jbh.commons.util.JbhMoneyUtils.JBH_ZERO;

import com.jbh.commons.util.JbhMoneyUtils;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import lombok.Builder;

@Builder(builderMethodName = "notUseThisInternalBuilder")
public record MonthlyBalanceDTO(
    Long id,
    ProductId productId,
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
      final ProductId accountId,
      final YearMonth period,
      final BigDecimal closingBalance,
      final boolean gapPeriod) {

    return defaultBuilder()
        .productId(accountId)
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

  public boolean isProfitable() {
    return !JbhMoneyUtils.isNegative(monthlyNetProfit);
  }

  public boolean isLoss() {
    return JbhMoneyUtils.isNegative(monthlyNetProfit);
  }
}
