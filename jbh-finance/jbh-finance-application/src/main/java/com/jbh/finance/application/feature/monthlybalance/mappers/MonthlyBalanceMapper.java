package com.jbh.finance.application.feature.monthlybalance.mappers;

import com.jbh.finance.application.feature.monthlybalance.dto.MonthlyBalanceDTO;
import com.jbh.finance.domain.monthlybalance.MonthlyBalanceDomain;

public final class MonthlyBalanceMapper {

  private MonthlyBalanceMapper() {}

  public static MonthlyBalanceDTO toDTO(final MonthlyBalanceDomain domain) {
    if (domain == null) {
      return null;
    }

    return MonthlyBalanceDTO.defaultBuilder()
        .id(domain.getId())
        .productId(domain.getProductId())
        .year(domain.getYear())
        .month(domain.getMonth())
        .period(domain.getPeriod())
        .totalDebits(domain.getTotalDebits())
        .totalCredits(domain.getTotalCredits())
        .movementBalance(domain.getMovementBalance())
        .openingBalance(domain.getOpeningBalance())
        .closingBalance(domain.getClosingBalance())
        .monthlyNetProfit(domain.getMonthlyNetProfit())
        .totalMovements(domain.getTotalMovements())
        .gapPeriod(domain.isGapPeriod())
        .officialMonthlyReport(domain.isOfficialMonthlyReport())
        .monthlyReportedProfit(domain.getMonthlyProfitReported())
        .netGrowthRate(domain.getNetGrowthRate())
        .incomeWithholdingTaxAmount(domain.getIncomeWithholdingTaxAmount())
        .build();
  }

  public static MonthlyBalanceDomain toDomain(final MonthlyBalanceDTO dto) {
    if (dto == null) {
      return null;
    }

    return new MonthlyBalanceDomain(
        dto.id(),
        dto.productId(),
        dto.year(),
        dto.month(),
        dto.period(),
        dto.netGrowthRate(),
        dto.totalDebits(),
        dto.totalCredits(),
        dto.openingBalance(),
        dto.closingBalance(),
        dto.monthlyNetProfit(),
        dto.totalMovements(),
        dto.gapPeriod(),
        dto.officialMonthlyReport(),
        dto.monthlyReportedProfit(),
        dto.incomeWithholdingTaxAmount());
  }
}
