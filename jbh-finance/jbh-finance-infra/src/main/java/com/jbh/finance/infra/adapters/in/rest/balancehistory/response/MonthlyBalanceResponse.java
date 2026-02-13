package com.jbh.finance.infra.adapters.in.rest.balancehistory.response;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.domain.product.vo.ProductId;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;

public record MonthlyBalanceResponse(
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
  public static MonthlyBalanceResponse fromDTO(final MonthlyBalanceDTO dto) {
    return new MonthlyBalanceResponse(
        dto.id(),
        dto.productId(),
        dto.year(),
        dto.month(),
        dto.period(),
        dto.netGrowthRate(),
        dto.totalDebits(),
        dto.totalCredits(),
        dto.movementBalance(),
        dto.openingBalance(),
        dto.closingBalance(),
        dto.monthlyNetProfit(),
        dto.totalMovements(),
        dto.gapPeriod(),
        dto.officialMonthlyReport(),
        dto.monthlyReportedProfit(),
        dto.incomeWithholdingTaxAmount(),
        dto.createdAt(),
        dto.updatedAt());
  }
}
