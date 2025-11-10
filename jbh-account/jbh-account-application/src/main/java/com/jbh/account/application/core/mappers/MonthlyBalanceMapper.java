package com.jbh.account.application.core.mappers;

import com.jbh.account.application.core.dto.MonthlyBalanceDTO;
import com.jbh.account.domain.entity.MonthlyBalanceDomain;

public final class MonthlyBalanceMapper {

  private MonthlyBalanceMapper() {}

  public static MonthlyBalanceDTO toDTO(final MonthlyBalanceDomain domain) {
    if (domain == null) {
      return null;
    }

    return MonthlyBalanceDTO.defaultBuilder()
        .id(domain.getId())
        .accountId(domain.getAccountId())
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
        dto.accountId(),
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
